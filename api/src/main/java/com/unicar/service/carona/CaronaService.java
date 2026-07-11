package com.unicar.service.carona;

import com.unicar.domain.Carona;
import com.unicar.domain.ReservaCarona;
import com.unicar.domain.Usuario;
import com.unicar.domain.Veiculo;
import com.unicar.dto.carona.CaronaDetalheResponseDTO;
import com.unicar.dto.carona.CaronaListItemResponseDTO;
import com.unicar.dto.carona.CaronaProximaResponseDTO;
import com.unicar.dto.carona.CaronaRequestDTO;
import com.unicar.dto.carona.CaronaResponseDTO;
import com.unicar.dto.carona.EnderecoDTO;
import com.unicar.dto.carona.MotoristaResumoDTO;
import com.unicar.dto.carona.PassageiroResponseDTO;
import com.unicar.dto.carona.VeiculoResumoDTO;
import com.unicar.enums.StatusCarona;
import com.unicar.enums.StatusReserva;
import com.unicar.exception.AcessoNegadoException;
import com.unicar.exception.CaronaNaoEncontradaException;
import com.unicar.exception.EstadoInvalidoException;
import com.unicar.exception.RegraDeNegocioException;
import com.unicar.exception.VeiculoNaoEncontradoException;
import com.unicar.repository.CaronaRepository;
import com.unicar.repository.CaronaRepository.CaronaProximaProjection;
import com.unicar.repository.ReservaCaronaRepository;
import com.unicar.repository.UsuarioRepository;
import com.unicar.repository.VeiculoRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CaronaService {
    private static final List<StatusReserva> RESERVAS_ATIVAS = List.of(StatusReserva.PENDENTE, StatusReserva.ACEITA);
    private static final double RAIO_PADRAO_KM = 5.0;
    private static final double RAIO_MAXIMO_KM = 100.0;
    private final CaronaRepository caronaRepository;
    private final UsuarioRepository usuarioRepository;
    private final VeiculoRepository veiculoRepository;
    private final ReservaCaronaRepository reservaCaronaRepository;

    @Value("${unicar.carona.fator-valor-por-km:1.00}")
    private BigDecimal fatorValorPorKm;

    @Transactional
    public CaronaResponseDTO criar(CaronaRequestDTO request, Long motoristaId) {
        Usuario motorista = usuarioRepository.findByIdForUpdate(motoristaId)
            .orElseThrow(() -> new AcessoNegadoException("Usuário não encontrado"));

        Veiculo veiculo = veiculoRepository.findById(request.veiculoId())
                .orElseThrow(() -> new VeiculoNaoEncontradoException("Veículo não encontrado"));

        if (!veiculo.getUsuario().getId().equals(motoristaId)) {
            throw new AcessoNegadoException("O veículo não pertence ao usuário autenticado");
        }

        if (!request.dataHoraSaida().isAfter(LocalDateTime.now())) {
            throw new RegraDeNegocioException("A data da viagem deve ser futura");
        }

        if (request.quantidadeVagas() == null || request.quantidadeVagas() <= 0) {
            throw new RegraDeNegocioException("A quantidade de vagas deve ser maior que zero");
        }

        boolean existeCaronaEmAndamento = caronaRepository.existsByMotorista_IdAndStatus(motoristaId, StatusCarona.EM_ANDAMENTO);
        if (existeCaronaEmAndamento) {
            throw new RegraDeNegocioException("O motorista já possui uma carona em andamento");
        }

        validarValorContribuicao(request.origem(), request.destino(), request.valorContribuicao());

        Carona carona = Carona.builder()
                .motorista(motorista)
                .veiculo(veiculo)
                .origemDescricao(request.origem().descricao())
                .origemLatitude(request.origem().latitude())
                .origemLongitude(request.origem().longitude())
                .destinoDescricao(request.destino().descricao())
                .destinoLatitude(request.destino().latitude())
                .destinoLongitude(request.destino().longitude())
                .pontoEncontroDescricao(request.pontoEncontro())
                .dataHoraPartida(request.dataHoraSaida())
                .vagasTotais(request.quantidadeVagas())
                .valorContribuicao(request.valorContribuicao())
                .status(StatusCarona.CRIADA)
                .build();

        carona = caronaRepository.save(carona);
        return new CaronaResponseDTO(carona.getId(), carona.getStatus());
    }


    public List<CaronaListItemResponseDTO> listarMinhas(Long motoristaId) {
        return caronaRepository.findByMotorista_Id(motoristaId).stream()
                .map(c -> new CaronaListItemResponseDTO(
                        c.getId(),
                        new EnderecoDTO(c.getOrigemDescricao(), c.getOrigemLatitude(), c.getOrigemLongitude()),
                        new EnderecoDTO(c.getDestinoDescricao(), c.getDestinoLatitude(), c.getDestinoLongitude()),
                        c.getStatus(),
                        c.getDataHoraPartida()))
                .toList();
    }

    public CaronaDetalheResponseDTO buscarPorId(Long id) {
        Carona carona = buscarCarona(id);

        int vagasOcupadas = contarPassageirosConfirmados(id);
        int vagasDisponiveis = carona.getVagasTotais() - vagasOcupadas;

        return new CaronaDetalheResponseDTO(
                carona.getId(),
                new EnderecoDTO(carona.getOrigemDescricao(), carona.getOrigemLatitude(), carona.getOrigemLongitude()),
                new EnderecoDTO(carona.getDestinoDescricao(), carona.getDestinoLatitude(), carona.getDestinoLongitude()),
                carona.getPontoEncontroDescricao(),
                carona.getDataHoraPartida(),
                carona.getVagasTotais(),
                vagasDisponiveis,
                carona.getValorContribuicao(),
                carona.getStatus(),
                new MotoristaResumoDTO(carona.getMotorista().getId(), carona.getMotorista().getNome()),
                new VeiculoResumoDTO(carona.getVeiculo().getId(), carona.getVeiculo().getModelo(), carona.getVeiculo().getCor())
        );
    }

    public List<CaronaProximaResponseDTO> buscarProximas(BigDecimal latitude, BigDecimal longitude, Double raioKm) {
        if (latitude == null || longitude == null) {
            throw new RegraDeNegocioException("Latitude e longitude são obrigatórias para a busca por proximidade");
        }
 
        double raio = (raioKm != null && raioKm > 0) ? raioKm : RAIO_PADRAO_KM;
        if (raio > RAIO_MAXIMO_KM) {
            throw new RegraDeNegocioException("O raio de busca não pode ultrapassar " + RAIO_MAXIMO_KM + " km");
        }
 
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

    @Transactional
    public CaronaResponseDTO atualizar(Long id, CaronaRequestDTO request, Long motoristaId) {
        Carona carona = buscarCaronaParaAtualizacao(id);
        validarMotorista(carona, motoristaId);

        if (carona.getStatus() != StatusCarona.CRIADA) {
            throw new RegraDeNegocioException("Não é possível editar a carona após o início da viagem");
        }

        if (!request.dataHoraSaida().isAfter(LocalDateTime.now())) {
            throw new RegraDeNegocioException("A data da viagem deve ser futura");
        }

        if (request.quantidadeVagas() == null || request.quantidadeVagas() <= 0) {
            throw new RegraDeNegocioException("A quantidade de vagas deve ser maior que zero");
        }

        int passageirosAceitos = contarPassageirosConfirmados(id);
        if (request.quantidadeVagas() < passageirosAceitos) {
            throw new RegraDeNegocioException(
                    "Não é possível reduzir as vagas abaixo da quantidade de passageiros já aceitos");
        }

        validarValorContribuicao(request.origem(), request.destino(), request.valorContribuicao());

        carona.setOrigemDescricao(request.origem().descricao());
        carona.setOrigemLatitude(request.origem().latitude());
        carona.setOrigemLongitude(request.origem().longitude());
        carona.setDestinoDescricao(request.destino().descricao());
        carona.setDestinoLatitude(request.destino().latitude());
        carona.setDestinoLongitude(request.destino().longitude());
        carona.setPontoEncontroDescricao(request.pontoEncontro());
        carona.setDataHoraPartida(request.dataHoraSaida());
        carona.setVagasTotais(request.quantidadeVagas());
        carona.setValorContribuicao(request.valorContribuicao());

        carona = caronaRepository.save(carona);
        return new CaronaResponseDTO(carona.getId(), carona.getStatus());
    }

    @Transactional
    public CaronaResponseDTO cancelar(Long id, Long motoristaId) {
        Carona carona = buscarCaronaParaAtualizacao(id);
        validarMotorista(carona, motoristaId);

        if (carona.getStatus() == StatusCarona.FINALIZADA
                || carona.getStatus() == StatusCarona.CANCELADA
                || carona.getStatus() == StatusCarona.EM_ANDAMENTO) {
            throw new EstadoInvalidoException("Não é possível cancelar uma carona com status " + carona.getStatus());
        }

        carona.setStatus(StatusCarona.CANCELADA);
        caronaRepository.save(carona);

        List<ReservaCarona> reservas = reservaCaronaRepository.findByCaronaIdAndStatusIn(id, RESERVAS_ATIVAS);
        LocalDateTime agora = LocalDateTime.now();
        reservas.forEach(reserva -> {
            reserva.setStatus(StatusReserva.CANCELADA);
            reserva.setDataResposta(agora);
        });
        reservaCaronaRepository.saveAll(reservas);
        return new CaronaResponseDTO(carona.getId(), carona.getStatus());
    }

    public List<PassageiroResponseDTO> listarPassageiros(Long caronaId, Long usuarioId) {
        Carona carona = buscarCarona(caronaId);
        validarMotorista(carona, usuarioId);
        List<ReservaCarona> reservas = reservaCaronaRepository.findByCaronaIdAndStatus(caronaId, StatusReserva.ACEITA);

        return reservas.stream()
                .map(PassageiroResponseDTO::new)
                .toList();
    }

    @Transactional
    public void iniciarCarona(Long caronaId, Long usuarioId) {
        Carona carona = buscarCaronaParaAtualizacao(caronaId);
        validarMotorista(carona, usuarioId);

        if (carona.getStatus() != StatusCarona.CRIADA) {
            throw new EstadoInvalidoException("Só é possível iniciar caronas com status CRIADA. Status atual: " + carona.getStatus());
        }

        LocalDate hoje = LocalDate.now();
        LocalDate diaPartida = carona.getDataHoraPartida().toLocalDate();

        if (!hoje.isEqual(diaPartida)) {
            String mensagem = hoje.isBefore(diaPartida)
                    ? "A carona está agendada para uma data futura. Aguarde o dia da viagem para iniciá-la."
                    : "A data da viagem já passou. Atualize a carona para uma nova data ou crie uma nova carona.";
            throw new RegraDeNegocioException(mensagem);
        }

        carona.setStatus(StatusCarona.EM_ANDAMENTO);
        caronaRepository.save(carona);
    }

    @Transactional
    public void finalizarCarona(Long caronaId, Long usuarioId) {
        Carona carona = buscarCaronaParaAtualizacao(caronaId);
        validarMotorista(carona, usuarioId);

        if (carona.getStatus() != StatusCarona.EM_ANDAMENTO) {
            throw new EstadoInvalidoException("Só é possível finalizar caronas com status EM_ANDAMENTO. Status atual: " + carona.getStatus());
        }

        carona.setStatus(StatusCarona.FINALIZADA);
        caronaRepository.save(carona);

        List<ReservaCarona> reservas = reservaCaronaRepository.findByCaronaIdAndStatus(caronaId, StatusReserva.ACEITA);
        reservas.forEach(r -> r.setStatus(StatusReserva.CONCLUIDA));
        reservaCaronaRepository.saveAll(reservas);
    }

    private Carona buscarCaronaParaAtualizacao(Long id) {
        return caronaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new CaronaNaoEncontradaException("Carona não encontrada: id=" + id));
    }

    private int contarPassageirosConfirmados(Long caronaId) {
        return reservaCaronaRepository.countByCarona_IdAndStatus(caronaId, StatusReserva.ACEITA);
    }

    private void validarValorContribuicao(EnderecoDTO origem, EnderecoDTO destino, BigDecimal valorContribuicao) {
        BigDecimal distanciaKm = calcularDistanciaKm(
                origem.latitude(), origem.longitude(),
                destino.latitude(), destino.longitude());
 
        BigDecimal valorMaximo = distanciaKm.multiply(fatorValorPorKm).setScale(2, RoundingMode.HALF_UP);
 
        if (valorContribuicao.compareTo(valorMaximo) > 0) {
            throw new RegraDeNegocioException(
                    String.format(
                            "O valor de contribuição (R$ %.2f) ultrapassa o limite permitido de R$ %.2f para %.2f km",
                            valorContribuicao, valorMaximo, distanciaKm));
        }
    }

    /**
     * Calcula a distância em km entre dois pontos usando Haversine.
     * Usado para validar a contribuição antes de salvar a carona.
     * A busca por caronas próximas usa earthdistance/cube no banco,
     * pois o cálculo é executado sobre várias caronas cadastradas.
     * Por isso, não é aplicado aqui.
     */
    private BigDecimal calcularDistanciaKm(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        final double raioTerraKm = 6371;

        double lat1Rad = Math.toRadians(lat1.doubleValue());
        double lat2Rad = Math.toRadians(lat2.doubleValue());
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distancia = raioTerraKm * c;

        return BigDecimal.valueOf(distancia).setScale(2, RoundingMode.HALF_UP);
    }

    private Carona buscarCarona(Long id) {
        return caronaRepository.findById(id)
                .orElseThrow(() -> new CaronaNaoEncontradaException("Carona não encontrada: id=" + id));
    }

    private void validarMotorista(Carona carona, Long usuarioId) {
        if (!carona.getMotorista().getId().equals(usuarioId)) {
            throw new AcessoNegadoException("Usuário não é o motorista desta carona");
        }
    }
}