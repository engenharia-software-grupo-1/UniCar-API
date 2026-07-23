package com.unicar.service.carona;

import com.unicar.domain.Carona;
import com.unicar.dto.avaliacao.ReputacaoDTO;
import com.unicar.dto.carona.*;
import com.unicar.exception.RegraDeNegocioException;
import com.unicar.repository.CaronaRepository;
import com.unicar.repository.CaronaRepository.CaronaProximaProjection;
import com.unicar.repository.ReservaCaronaRepository;
import com.unicar.service.AvaliacaoService;
import com.unicar.util.GeoUtils;
import lombok.RequiredArgsConstructor;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Responsável por todas as formas de busca de caronas disponíveis
 * (por filtros dinâmicos ou por proximidade simples), separado do
 * CaronaService, que cuida do ciclo de vida da carona em si.
 */
@Service
@RequiredArgsConstructor
public class BuscaCaronaService {

    private static final double RAIO_PADRAO_KM = 5.0;
    private static final double RAIO_MAXIMO_KM = 100.0;

    private final CaronaRepository caronaRepository;
    private final ReservaCaronaRepository reservaCaronaRepository;
    private final AvaliacaoService avaliacaoService;

    /**
     * Busca caronas disponíveis para o usuário autenticado, aplicando filtros
     * de proximidade, data, gênero/curso do motorista e bloqueios entre usuários.
     */
    public List<CaronaBuscaResponseDTO> buscarCaronasDisponiveis(BuscaCaronaFiltroDTO filtro, Long usuarioAutenticadoId) {
        double raioEfetivo = validarERetornarRaio(filtro.raioKm());

        Specification<Carona> spec = montarSpecification(filtro, usuarioAutenticadoId, raioEfetivo);
        List<Carona> candidatas = caronaRepository.findAll(spec);

        List<Carona> dentroDoRaio = filtrarPorDistanciaExata(candidatas, filtro, raioEfetivo);

        Map<Long, ReputacaoDTO> reputacoesPorMotorista = buscarReputacoesEmLote(dentroDoRaio);

        return dentroDoRaio.stream()
                .map(c -> mapearParaBuscaResponseDTO(c, reputacoesPorMotorista.get(c.getMotorista().getId())))
                .toList();
    }

    /**
     * Busca simples de caronas próximas por coordenadas, usando earthdistance/cube
     * no banco. Usado como alternativa mais leve à busca com filtros dinâmicos.
     */
    public List<CaronaProximaResponseDTO> buscarProximas(BigDecimal latitude, BigDecimal longitude, Double raioKm) {
        if (latitude == null || longitude == null) {
            throw new RegraDeNegocioException("Latitude e longitude são obrigatórias para a busca por proximidade");
        }

        double raio = validarERetornarRaio(raioKm);
        double raioMetros = raio * 1000;

        List<CaronaProximaProjection> caronasProximas =
                caronaRepository.buscarCaronasProximas(latitude, longitude, raioMetros);

        return caronasProximas.stream()
                .map(p -> new CaronaProximaResponseDTO(
                        p.getId(),
                        new EnderecoDTO(p.getOrigemDescricao(), p.getOrigemLatitude(), p.getOrigemLongitude()),
                        new EnderecoDTO(p.getDestinoDescricao(), p.getDestinoLatitude(), p.getDestinoLongitude()),
                        p.getStatus(),
                        p.getDataHoraPartida(),
                        p.getVagasTotais() - contarPassageirosConfirmados(p.getId()),
                        BigDecimal.valueOf(p.getDistanciaKm()).setScale(2, RoundingMode.HALF_UP)))
                .filter(dto -> dto.vagasDisponiveis() > 0)
                .toList();
    }

    /**
     * Define o raio de busca (usa o padrão se não informado) e valida
     * que não ultrapassa o limite máximo permitido.
     */
    private double validarERetornarRaio(Double raioKm) {
        double raio = (raioKm != null && raioKm > 0) ? raioKm : RAIO_PADRAO_KM;
        if (raio > RAIO_MAXIMO_KM) {
            throw new RegraDeNegocioException("O raio de busca não pode ultrapassar " + RAIO_MAXIMO_KM + " km");
        }
        return raio;
    }

    /**
     * Combina todos os filtros da busca (status, data, vagas, bloqueios,
     * bounding box geográfico, gênero e horário) em uma única Specification.
     */
    private Specification<Carona> montarSpecification(BuscaCaronaFiltroDTO filtro, Long usuarioAutenticadoId, double raioEfetivo) {
        return Specification.where(CaronaSpecifications.comStatusCriada())
                .and(CaronaSpecifications.comDataFutura())
                .and(CaronaSpecifications.comVagasDisponiveis())
                .and(CaronaSpecifications.semBloqueioBidirecional(usuarioAutenticadoId))
                .and(CaronaSpecifications.comBoundingBox(filtro.origemLatitude(), filtro.origemLongitude(), raioEfetivo))
                .and(CaronaSpecifications.comGeneroMotorista(filtro.generoMotorista()))
                .and(CaronaSpecifications.comCursoMotorista(filtro.cursoMotorista()))
                .and(CaronaSpecifications.comDataHoraSaida(filtro.dataHoraSaida()));
    }

    /**
     * Refina o resultado do bounding box aplicando o cálculo exato de Haversine,
     * garantindo que só entrem caronas realmente dentro do raio pedido.
     */
    private List<Carona> filtrarPorDistanciaExata(List<Carona> candidatas, BuscaCaronaFiltroDTO filtro, double raioEfetivo) {
        if (filtro.origemLatitude() == null || filtro.origemLongitude() == null) {
            return candidatas;
        }
        return candidatas.stream()
                .filter(c -> GeoUtils.calcularDistanciaKm(
                        filtro.origemLatitude(), filtro.origemLongitude(),
                        c.getOrigemLatitude(), c.getOrigemLongitude())
                        .doubleValue() <= raioEfetivo)
                .toList();
    }

    /**
     * Busca a reputação de todos os motoristas das caronas candidatas
     * em uma única chamada ao AvaliacaoService, evitando N chamadas repetidas.
     * Retorna vazio sem chamar o serviço quando não há candidatas.
     */
    private Map<Long, ReputacaoDTO> buscarReputacoesEmLote(List<Carona> caronas) {
        if (caronas.isEmpty()) {
            return Map.of();
        }

        List<Long> motoristaIds = caronas.stream()
                .map(c -> c.getMotorista().getId())
                .distinct()
                .toList();

        return avaliacaoService.buscarReputacoes(motoristaIds).stream()
                .collect(Collectors.toMap(ReputacaoDTO::usuarioId, r -> r));
    }

    /**
     * Converte uma entidade Carona no DTO de resposta da busca,
     * incluindo a reputação do motorista buscada no AvaliacaoService.
     */
    private CaronaBuscaResponseDTO mapearParaBuscaResponseDTO(Carona c, ReputacaoDTO reputacao) {
        return new CaronaBuscaResponseDTO(
                c.getId(),
                new EnderecoDTO(c.getOrigemDescricao(), c.getOrigemLatitude(), c.getOrigemLongitude()),
                new EnderecoDTO(c.getDestinoDescricao(), c.getDestinoLatitude(), c.getDestinoLongitude()),
                new MotoristaBuscaDTO(
                        c.getMotorista().getId(),
                        c.getMotorista().getNome(),
                        c.getMotorista().getGenero() != null ? c.getMotorista().getGenero().name() : null,
                        c.getMotorista().getCurso(),
                        c.getMotorista().getLinkFoto(),
                        reputacao != null ? reputacao.media() : null),
                c.getDataHoraPartida(),
                c.getVagasTotais(),
                c.getValorContribuicao());
    }

    private int contarPassageirosConfirmados(Long caronaId) {
        return reservaCaronaRepository.somarPassageirosPorCaronaEStatus(caronaId, com.unicar.enums.StatusReserva.ACEITA);
    }
}