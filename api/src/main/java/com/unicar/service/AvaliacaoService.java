package com.unicar.service;

import com.unicar.domain.Avaliacao;
import com.unicar.domain.Carona;
import com.unicar.domain.Usuario;
import com.unicar.dto.avaliacao.AvaliacaoRecebidaDTO;
import com.unicar.dto.avaliacao.AvaliacaoRequestDTO;
import com.unicar.dto.avaliacao.ParticipantePendenteDTO;
import com.unicar.dto.avaliacao.ReputacaoDTO;
import com.unicar.enums.StatusCarona;
import com.unicar.exception.CaronaNaoEncontradaException;
import com.unicar.exception.RegraDeNegocioException;
import com.unicar.exception.UsuarioNaoEncontradoException;
import com.unicar.repository.AvaliacaoRepository;
import com.unicar.repository.AvaliacaoRepository.ReputacaoAgregadaProjection;
import com.unicar.repository.CaronaRepository;
import com.unicar.repository.ReservaCaronaRepository;
import com.unicar.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CaronaRepository caronaRepository;
    private final ReservaCaronaRepository reservaCaronaRepository;

    /**
     * Registra a avaliação de um usuário sobre outro em uma carona finalizada,
     * validando participação, nota e se já não foi avaliado antes.
     */
    @Transactional
    public void avaliar(Long usuarioId, AvaliacaoRequestDTO dto) {

        Usuario avaliador = buscarUsuario(usuarioId);
        Usuario avaliado = buscarUsuario(dto.avaliadoId());
        Carona carona = buscarCarona(dto.caronaId());

        validarNota(dto.nota());
        validarCaronaFinalizada(carona);
        validarParticipacao(carona, avaliador.getId(), avaliado.getId());
        validarNaoAvaliouAntes(carona.getId(), avaliador.getId(), avaliado.getId());

        Avaliacao avaliacao = Avaliacao.builder()
                .carona(carona)
                .avaliador(avaliador)
                .avaliado(avaliado)
                .nota(dto.nota())
                .comentario(dto.comentario())
                .dataAvaliacao(LocalDateTime.now())
                .build();

        avaliacaoRepository.save(avaliacao);
    }

    /**
     * Lista todas as avaliações recebidas por um usuário.
     */
    public List<AvaliacaoRecebidaDTO> listarAvaliacoesRecebidas(Long usuarioId) {

        buscarUsuario(usuarioId);

        return avaliacaoRepository.findByAvaliadoId(usuarioId)
                .stream()
                .map(AvaliacaoRecebidaDTO::new)
                .toList();
    }

    /**
     * Calcula a reputação (média e quantidade de avaliações) de um único usuário.
     */
    public ReputacaoDTO buscarReputacao(Long usuarioId) {

        Usuario usuario = buscarUsuario(usuarioId);

        Double media = avaliacaoRepository.calcularMedia(usuarioId);
        Long quantidade = avaliacaoRepository.countByAvaliadoId(usuarioId);

        return new ReputacaoDTO(
                usuario.getId(),
                media == null ? 0.0 : media,
                quantidade
        );
    }

    /**
     * Calcula a reputação de vários usuários em uma única query agregada,
     * evitando N chamadas ao banco quando há múltiplos motoristas a avaliar
     * (usado, por exemplo, na busca de caronas disponíveis).
     * Usuários sem avaliação retornam média 0.0 e quantidade 0.
     */
    public List<ReputacaoDTO> buscarReputacoes(List<Long> usuarioIds) {
        if (usuarioIds == null || usuarioIds.isEmpty()) {
            return List.of();
        }

        List<ReputacaoAgregadaProjection> agregadas =
                avaliacaoRepository.calcularMediasPorUsuarios(usuarioIds);

        Map<Long, ReputacaoAgregadaProjection> porUsuario = agregadas.stream()
                .collect(Collectors.toMap(ReputacaoAgregadaProjection::getUsuarioId, r -> r));

        return usuarioIds.stream()
                .map(id -> {
                    ReputacaoAgregadaProjection r = porUsuario.get(id);
                    double media = (r != null && r.getMedia() != null) ? r.getMedia() : 0.0;
                    long quantidade = (r != null) ? r.getQuantidade() : 0L;
                    return new ReputacaoDTO(id, media, quantidade);
                })
                .toList();
    }

    /**
     * Lista os participantes de uma carona finalizada que o usuário autenticado ainda não avaliou.
     */
    public List<ParticipantePendenteDTO> listarAvaliacoesPendentes(Long caronaId, Long usuarioAutenticadoId) {
        Carona carona = buscarCarona(caronaId);
        validarCaronaFinalizada(carona);

        // Reutiliza seu método de validação para garantir segurança na consulta
        Usuario usuarioLogado = buscarUsuario(usuarioAutenticadoId);

        java.util.List<ParticipantePendenteDTO> pendentes = new java.util.ArrayList<>();

        boolean ehMotorista = carona.getMotorista().getId().equals(usuarioAutenticadoId);

        if (!ehMotorista) {
            // Se eu sou PASSAGEIRO, posso avaliar o MOTORISTA caso ainda não o tenha feito
            boolean jaAvaliouMotorista = avaliacaoRepository.existsByCaronaIdAndAvaliadorIdAndAvaliadoId(
                    caronaId, usuarioAutenticadoId, carona.getMotorista().getId()
            );
            if (!jaAvaliouMotorista) {
                pendentes.add(new ParticipantePendenteDTO(
                        carona.getMotorista().getId(),
                        carona.getMotorista().getNome(),
                        "MOTORISTA"
                ));
            }
        } else {
            // Se eu sou MOTORISTA, preciso buscar todos os passageiros confirmados desta carona
            // Nota: use o método adequado do seu reservaCaronaRepository que traga os passageiros aceitos
            List<com.unicar.domain.ReservaCarona> reservas = reservaCaronaRepository.findByCaronaId(caronaId);

            for (com.unicar.domain.ReservaCarona reserva : reservas) {
                Long passageiroId = reserva.getUsuario().getId();
                boolean jaAvaliouPassageiro = avaliacaoRepository.existsByCaronaIdAndAvaliadorIdAndAvaliadoId(
                        caronaId, usuarioAutenticadoId, passageiroId
                );
                if (!jaAvaliouPassageiro) {
                    pendentes.add(new ParticipantePendenteDTO(
                            passageiroId,
                            reserva.getUsuario().getNome(),
                            "PASSAGEIRO"
                    ));
                }
            }
        }

        return pendentes;
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new UsuarioNaoEncontradoException("Usuário não encontrado."));
    }

    private Carona buscarCarona(Long id) {
        return caronaRepository.findById(id)
                .orElseThrow(() ->
                        new CaronaNaoEncontradaException("Carona não encontrada."));
    }

    private void validarNota(Integer nota) {
        if (nota < 1 || nota > 5) {
            throw new RegraDeNegocioException(
                    "A nota deve estar entre 1 e 5.");
        }
    }

    private void validarCaronaFinalizada(Carona carona) {
        if (!carona.getStatus().equals(StatusCarona.FINALIZADA)) {
            throw new RegraDeNegocioException(
                    "A carona ainda não foi finalizada.");
        }
    }

    private void validarParticipacao(Carona carona,
                                     Long avaliadorId,
                                     Long avaliadoId) {

        boolean avaliadorEhMotorista =
                carona.getMotorista().getId().equals(avaliadorId);

        boolean avaliadoEhMotorista =
                carona.getMotorista().getId().equals(avaliadoId);

        boolean avaliadorEhPassageiro =
                reservaCaronaRepository.existsByCaronaIdAndUsuarioId(
                        carona.getId(), avaliadorId);

        boolean avaliadoEhPassageiro =
                reservaCaronaRepository.existsByCaronaIdAndUsuarioId(
                        carona.getId(), avaliadoId);

        if (!(avaliadorEhMotorista || avaliadorEhPassageiro)) {
            throw new RegraDeNegocioException(
                    "O avaliador não participou da carona.");
        }

        if (!(avaliadoEhMotorista || avaliadoEhPassageiro)) {
            throw new RegraDeNegocioException(
                    "O avaliado não participou da carona.");
        }

        if (avaliadorId.equals(avaliadoId)) {
            throw new RegraDeNegocioException(
                    "O usuário não pode avaliar a si mesmo.");
        }
        if (avaliadorEhPassageiro && avaliadoEhPassageiro) {
            throw new RegraDeNegocioException(
                "Passageiros não podem se avaliar entre si.");
        }
    }

    private void validarNaoAvaliouAntes(Long caronaId, Long avaliadorId, Long avaliadoId) {
        if (avaliacaoRepository.existsByCaronaIdAndAvaliadorIdAndAvaliadoId(caronaId, avaliadorId, avaliadoId)) {
            throw new RegraDeNegocioException("Você já avaliou este usuário nesta carona.");
        }
    }
}