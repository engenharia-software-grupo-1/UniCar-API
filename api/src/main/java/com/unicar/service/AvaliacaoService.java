package com.unicar.service;

import com.unicar.domain.Avaliacao;
import com.unicar.domain.Carona;
import com.unicar.domain.ReservaCarona;
import com.unicar.domain.Usuario;
import com.unicar.dto.avaliacao.AvaliacaoRecebidaDTO;
import com.unicar.dto.avaliacao.AvaliacaoRequestDTO;
import com.unicar.dto.avaliacao.ParticipantePendenteDTO;
import com.unicar.dto.avaliacao.ReputacaoDTO;
import com.unicar.enums.StatusCarona;
import com.unicar.enums.StatusReserva;
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

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    public Long avaliar(Long usuarioId, AvaliacaoRequestDTO dto) {
        Usuario avaliador = buscarUsuario(usuarioId);
        Usuario avaliado = buscarUsuario(dto.avaliadoId());
        Carona carona = buscarCarona(dto.caronaId());

        validarNota(dto.nota());
        validarParticipacao(carona, avaliador.getId(), avaliado.getId());
        validarCaronaFinalizada(carona);
        validarNaoAvaliouAntes(carona.getId(), avaliador.getId(), avaliado.getId());

        Avaliacao avaliacao = Avaliacao.builder()
                .carona(carona)
                .avaliador(avaliador)
                .avaliado(avaliado)
                .nota(dto.nota())
                .comentario(dto.comentario())
                .dataAvaliacao(LocalDateTime.now())
                .build();

        try {
            return avaliacaoRepository.saveAndFlush(avaliacao).getId();
        } catch (DataIntegrityViolationException e) {
            throw new RegraDeNegocioException("Você já avaliou este usuário nesta carona.");
        }
    }

    /**
     * Lista todas as avaliações recebidas por um usuário.
     */
    public List<AvaliacaoRecebidaDTO> listarAvaliacoesRecebidas(Long usuarioId) {
        buscarUsuario(usuarioId);

        return avaliacaoRepository.findByAvaliadoIdOrderByDataAvaliacaoDesc(usuarioId)
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

        List<AvaliacaoRecebidaDTO> avaliacoes = listarAvaliacoesRecebidas(usuarioId);

        return new ReputacaoDTO(
                usuario.getId(),
                arredondarMedia(media),
                quantidade,
                avaliacoes
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

        List<ReputacaoAgregadaProjection> agregadas = avaliacaoRepository.calcularMediasPorUsuarios(usuarioIds);
        Map<Long, ReputacaoAgregadaProjection> porUsuario = agregadas.stream()
                .collect(Collectors.toMap(ReputacaoAgregadaProjection::getUsuarioId, r -> r));

        return usuarioIds.stream()
                .map(id -> {
                    ReputacaoAgregadaProjection r = porUsuario.get(id);
                    Double mediaRaw = (r != null) ? r.getMedia() : null;
                    long quantidade = (r != null) ? r.getQuantidade() : 0L;
                    return new ReputacaoDTO(id, arredondarMedia(mediaRaw), quantidade);
                })
                .toList();
    }

    /**
     * Lista todas as avaliações pendentes de um usuário em uma carona finalizada.
     */
    public List<ParticipantePendenteDTO> listarAvaliacoesPendentes(Long caronaId, Long usuarioAutenticadoId) {
        Carona carona = buscarCarona(caronaId);

        List<ParticipantePendenteDTO> pendentes = new java.util.ArrayList<>();

        if (carona.getStatus() != StatusCarona.FINALIZADA) {
            // Carona ainda não finalizada (ou cancelada): não há avaliações pendentes ainda.
            return pendentes;
        }

        boolean ehMotorista = carona.getMotorista().getId().equals(usuarioAutenticadoId);
        boolean ehPassageiro = reservaCaronaRepository
                .existsByCaronaIdAndUsuarioIdAndStatus(caronaId, usuarioAutenticadoId, StatusReserva.CONCLUIDA);

        if (!ehMotorista && !ehPassageiro) {
            throw new RegraDeNegocioException("Apenas participantes da carona podem consultar avaliações pendentes.");
        }

        if (!ehMotorista) {
            boolean jaAvaliouMotorista = avaliacaoRepository.existsByCaronaIdAndAvaliadorIdAndAvaliadoId(
                    caronaId, usuarioAutenticadoId, carona.getMotorista().getId()
            );
            if (!jaAvaliouMotorista) {
                pendentes.add(new ParticipantePendenteDTO(
                        carona.getMotorista().getId(),
                        carona.getMotorista().getNome(),
                        carona.getMotorista().getLinkFoto(),
                        "MOTORISTA"
                ));
            }
        } else {
            List<ReservaCarona> reservas = reservaCaronaRepository
                    .findByCaronaIdAndStatus(caronaId, StatusReserva.CONCLUIDA);

            List<Long> jaAvaliados = avaliacaoRepository
                    .findAvaliadoIdsByCaronaIdAndAvaliadorId(caronaId, usuarioAutenticadoId);
            Set<Long> jaAvaliadosSet = new HashSet<>(jaAvaliados);

            for (ReservaCarona reserva : reservas) {
                try {
                    Long passageiroId = reserva.getUsuario().getId();
                    if (!jaAvaliadosSet.contains(passageiroId)) {
                        pendentes.add(new ParticipantePendenteDTO(
                                passageiroId,
                                reserva.getUsuario().getNome(),
                                reserva.getUsuario().getLinkFoto(),
                                "PASSAGEIRO"
                        ));
                    }
                } catch (Exception e) {
                    // não interrompe o loop, as demais pendências continuam sendo retornadas
                }
            }
        }

        return pendentes;
    }

    public Long contaAvaliacoes(Long id){
        return avaliacaoRepository.countByAvaliadoId(id);
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado."));
    }

    private Carona buscarCarona(Long id) {
        return caronaRepository.findById(id)
                .orElseThrow(() -> new CaronaNaoEncontradaException("Carona não encontrada."));
    }

    private void validarNota(Integer nota) {
        if (nota < 1 || nota > 5) {
            throw new RegraDeNegocioException("A nota deve estar entre 1 e 5.");
        }
    }

    private void validarCaronaFinalizada(Carona carona) {
        if (!carona.getStatus().equals(StatusCarona.FINALIZADA)) {
            throw new RegraDeNegocioException("A carona ainda não foi finalizada.");
        }
    }

    private void validarParticipacao(Carona carona, Long avaliadorId, Long avaliadoId) {
        boolean avaliadorEhMotorista = carona.getMotorista().getId().equals(avaliadorId);
        boolean avaliadoEhMotorista = carona.getMotorista().getId().equals(avaliadoId);

        boolean avaliadorEhPassageiro = reservaCaronaRepository
                .existsByCaronaIdAndUsuarioIdAndStatus(carona.getId(), avaliadorId, StatusReserva.CONCLUIDA);
        boolean avaliadoEhPassageiro = reservaCaronaRepository
                .existsByCaronaIdAndUsuarioIdAndStatus(carona.getId(), avaliadoId, StatusReserva.CONCLUIDA);

        if (!(avaliadorEhMotorista || avaliadorEhPassageiro)) {
            throw new RegraDeNegocioException("O avaliador não participou da carona.");
        }
        if (!(avaliadoEhMotorista || avaliadoEhPassageiro)) {
            throw new RegraDeNegocioException("O avaliado não participou da carona.");
        }
        if (avaliadorId.equals(avaliadoId)) {
            throw new RegraDeNegocioException("O usuário não pode avaliar a si mesmo.");
        }
        if (avaliadoEhPassageiro && !avaliadorEhMotorista) {
            throw new RegraDeNegocioException("Apenas o motorista dono da carona pode avaliar um passageiro.");
        }
    }

    private void validarNaoAvaliouAntes(Long caronaId, Long avaliadorId, Long avaliadoId) {
        if (avaliacaoRepository.existsByCaronaIdAndAvaliadorIdAndAvaliadoId(caronaId, avaliadorId, avaliadoId)) {
            throw new RegraDeNegocioException("Você já avaliou este usuário nesta carona.");
        }
    }

    private Double arredondarMedia(Double media) {
        if (media == null) return 0.0;
        return BigDecimal.valueOf(media)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
