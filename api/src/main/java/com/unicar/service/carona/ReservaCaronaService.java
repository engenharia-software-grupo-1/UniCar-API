package com.unicar.service.carona;

import com.unicar.domain.Carona;
import com.unicar.domain.ReservaCarona;
import com.unicar.domain.Usuario;
import com.unicar.dto.carona.EnderecoDTO;
import com.unicar.dto.carona.ReservaDetalheResponseDTO;
import com.unicar.dto.carona.ReservaEnviadaResponseDTO;
import com.unicar.dto.carona.ReservaRecebidaResponseDTO;
import com.unicar.dto.carona.ReservaRequestDTO;
import com.unicar.dto.carona.ReservaResponseDTO;
import com.unicar.dto.carona.ReservaSimulacaoResponseDTO;
import com.unicar.enums.StatusCarona;
import com.unicar.enums.StatusReserva;
import com.unicar.exception.AcessoNegadoException;
import com.unicar.exception.CaronaNaoEncontradaException;
import com.unicar.exception.EstadoInvalidoException;
import com.unicar.exception.RegraDeNegocioException;
import com.unicar.exception.ReservaNaoEncontradaException;
import com.unicar.repository.CaronaRepository;
import com.unicar.repository.ReservaCaronaRepository;
import com.unicar.repository.UsuarioRepository;
import com.unicar.util.GeoUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservaCaronaService {

    private static final List<StatusReserva> STATUS_ATIVOS = List.of(StatusReserva.PENDENTE, StatusReserva.ACEITA);

    private final ReservaCaronaRepository repository;
    private final CaronaRepository caronaRepository;
    private final UsuarioRepository usuarioRepository;

    @Value("${unicar.reserva.tolerancia-trajeto-km:3.0}")
    private BigDecimal toleranciaTrajetoKm;

    @Transactional
    public ReservaResponseDTO solicitar(ReservaRequestDTO request, Long usuarioId) {
        Carona carona = buscarCaronaParaAtualizacao(request.caronaId());

        if (carona.getMotorista().getId().equals(usuarioId)) {
            throw new RegraDeNegocioException("O motorista não pode reservar sua própria carona");
        }

        if (carona.getStatus() != StatusCarona.CRIADA) {
            throw new EstadoInvalidoException("Apenas caronas com status CRIADA podem receber novas reservas");
        }

        int vagasOcupadas = repository.countByCarona_IdAndStatus(carona.getId(), StatusReserva.ACEITA);
        int vagasDisponiveis = carona.getVagasTotais() - vagasOcupadas;
        if (request.quantidadePassageiros() > vagasDisponiveis) {
            throw new RegraDeNegocioException("Quantidade de vagas indisponível");
        }

        if (repository.existsByCarona_IdAndUsuario_IdAndStatusIn(carona.getId(), usuarioId, STATUS_ATIVOS)) {
            throw new RegraDeNegocioException("Você já possui uma reserva ativa para esta carona");
        }

        BigDecimal valorContribuicao = calcularValorContribuicao(
                carona, request.origemEmbarque(), request.quantidadePassageiros());

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new AcessoNegadoException("Usuário não encontrado"));

        ReservaCarona reserva = ReservaCarona.builder()
                .carona(carona)
                .usuario(usuario)
                .quantidadePassageiros(request.quantidadePassageiros())
                .origemEmbarqueDescricao(request.origemEmbarque().descricao())
                .origemEmbarqueLatitude(request.origemEmbarque().latitude())
                .origemEmbarqueLongitude(request.origemEmbarque().longitude())
                .valorContribuicao(valorContribuicao)
                .status(StatusReserva.PENDENTE)
                .dataExpiracao(carona.getDataHoraPartida().minusHours(1))
                .build();

        reserva = repository.save(reserva);
        return new ReservaResponseDTO(reserva);
    }

    public ReservaSimulacaoResponseDTO simular(ReservaRequestDTO request) {
        Carona carona = buscarCarona(request.caronaId());
        BigDecimal valorContribuicao = calcularValorContribuicao(
                carona, request.origemEmbarque(), request.quantidadePassageiros());
        return new ReservaSimulacaoResponseDTO(valorContribuicao);
    }

    public List<ReservaEnviadaResponseDTO> listarEnviadas(Long usuarioId) {
        return repository.findByUsuario_Id(usuarioId).stream()
                .map(ReservaEnviadaResponseDTO::new)
                .toList();
    }

    public List<ReservaRecebidaResponseDTO> listarRecebidas(Long motoristaId) {
        return repository.findByCarona_Motorista_Id(motoristaId).stream()
                .map(ReservaRecebidaResponseDTO::new)
                .toList();
    }

    public ReservaDetalheResponseDTO buscarDetalhe(Long reservaId, Long usuarioId) {
        ReservaCarona reserva = buscarReserva(reservaId);

        boolean isPassageiro = reserva.getUsuario().getId().equals(usuarioId);
        boolean isMotorista = reserva.getCarona().getMotorista().getId().equals(usuarioId);
        if (!isPassageiro && !isMotorista) {
            throw new AcessoNegadoException("Usuário não tem permissão para consultar esta reserva");
        }

        return new ReservaDetalheResponseDTO(reserva);
    }

    @Transactional
    public void removerReserva(Long reservaId, Long usuarioId) {
        ReservaCarona reserva = buscarReserva(reservaId);
        validarDono(reserva, usuarioId);

        if (reserva.getStatus() != StatusReserva.PENDENTE && reserva.getStatus() != StatusReserva.ACEITA) {
            throw new EstadoInvalidoException("Não é possível remover uma reserva com status " + reserva.getStatus());
        }

        reserva.setStatus(StatusReserva.CANCELADA);
        repository.save(reserva);
    }

    public ReservaCarona buscarReserva(Long reservaId) {
        return repository.findById(reservaId).orElseThrow(() -> new ReservaNaoEncontradaException("Reserva não encontrada: id=" + reservaId));
    }

    private void validarDono(ReservaCarona reserva, Long usuarioId) {
        if (!reserva.getUsuario().getId().equals(usuarioId)) {
            throw new AcessoNegadoException("Usuário não é o dono desta reserva");
        }
    }

    private BigDecimal calcularValorContribuicao(Carona carona, EnderecoDTO origemEmbarque, Integer quantidadePassageiros) {
        BigDecimal distanciaTotal = GeoUtils.calcularDistanciaKm(
                carona.getOrigemLatitude(), carona.getOrigemLongitude(),
                carona.getDestinoLatitude(), carona.getDestinoLongitude());

        if (distanciaTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("Não é possível calcular o valor da contribuição para esta carona");
        }

        BigDecimal distanciaOrigemEmbarque = GeoUtils.calcularDistanciaKm(
                carona.getOrigemLatitude(), carona.getOrigemLongitude(),
                origemEmbarque.latitude(), origemEmbarque.longitude());

        BigDecimal distanciaEmbarqueDestino = GeoUtils.calcularDistanciaKm(
                origemEmbarque.latitude(), origemEmbarque.longitude(),
                carona.getDestinoLatitude(), carona.getDestinoLongitude());

        BigDecimal desvioTrajeto = distanciaOrigemEmbarque.add(distanciaEmbarqueDestino).subtract(distanciaTotal);
        if (desvioTrajeto.compareTo(toleranciaTrajetoKm) > 0) {
            throw new RegraDeNegocioException("O local de embarque não é compatível com o trajeto da carona");
        }

        BigDecimal proporcaoTrecho = distanciaEmbarqueDestino.divide(distanciaTotal, 10, RoundingMode.HALF_UP);

        return carona.getValorContribuicao()
                .multiply(proporcaoTrecho)
                .multiply(BigDecimal.valueOf(quantidadePassageiros))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Carona buscarCarona(Long caronaId) {
        return caronaRepository.findById(caronaId)
                .orElseThrow(() -> new CaronaNaoEncontradaException("Carona não encontrada: id=" + caronaId));
    }

    private Carona buscarCaronaParaAtualizacao(Long caronaId) {
        return caronaRepository.findByIdForUpdate(caronaId)
                .orElseThrow(() -> new CaronaNaoEncontradaException("Carona não encontrada: id=" + caronaId));
    }
}
