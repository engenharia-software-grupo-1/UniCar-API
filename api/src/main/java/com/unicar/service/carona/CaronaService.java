package com.unicar.service.carona;

import com.unicar.domain.Carona;
import com.unicar.domain.InteresseTrajeto;
import com.unicar.domain.ReservaCarona;
import com.unicar.domain.Usuario;
import com.unicar.domain.Veiculo;
import com.unicar.dto.carona.*;
import com.unicar.enums.StatusCarona;
import com.unicar.enums.StatusReserva;
import com.unicar.enums.TipoNotificacao;
import com.unicar.exception.*;
import com.unicar.repository.*;
import com.unicar.service.NotificacaoService;
import com.unicar.util.GeoUtils;
import com.unicar.util.notificacoes.NotificacaoTemplates;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CaronaService {
    private static final List<StatusReserva> RESERVAS_ATIVAS = List.of(StatusReserva.PENDENTE, StatusReserva.ACEITA);

    private final CaronaRepository caronaRepository;
    private final UsuarioRepository usuarioRepository;
    private final VeiculoRepository veiculoRepository;
    private final ReservaCaronaRepository reservaCaronaRepository;
    private final InteresseTrajetoRepository interesseTrajetoRepository;
    private final NotificacaoService notificacaoService;

    @Value("${unicar.carona.preco-combustivel-litro:6.00}")
    private BigDecimal precoCombustivelLitro;

    @Value("${unicar.carona.consumo-medio-km-litro:12.00}")
    private BigDecimal consumoMedioKmLitro;

    @Value("${unicar.carona.margem-maxima-motorista:0.15}")
    private BigDecimal margemMaximaMotorista;

    @Transactional
    public List<CaronaResponseDTO> criar(CaronaRequestDTO request, Long motoristaId) {
        Usuario motorista = usuarioRepository.findByIdForUpdate(motoristaId)
                .orElseThrow(() -> new AcessoNegadoException("Usuário não encontrado"));

        Veiculo veiculo = veiculoRepository.findById(request.veiculoId())
                .orElseThrow(() -> new VeiculoNaoEncontradoException("Veículo não encontrado"));

        if (!veiculo.getUsuario().getId().equals(motoristaId)) {
            throw new AcessoNegadoException("O veículo não pertence ao usuário autenticado");
        }

        if (request.quantidadeVagas() == null || request.quantidadeVagas() <= 0) {
            throw new RegraDeNegocioException("A quantidade de vagas deve ser maior que zero");
        }

        boolean existeCaronaEmAndamento = caronaRepository.existsByMotorista_IdAndStatus(motoristaId, StatusCarona.EM_ANDAMENTO);
        if (existeCaronaEmAndamento) {
            throw new RegraDeNegocioException("O motorista já possui uma carona em andamento");
        }

        validarValorContribuicao(request.origem(), request.destino(), BigDecimal.ZERO, request.valorContribuicao());

        List<Carona> caronasParaSalvar = request.datasHorasSaida().stream()
                .map(dataHora -> {
                    if (!dataHora.isAfter(LocalDateTime.now())) {
                        throw new RegraDeNegocioException("Todas as datas da viagem devem ser futuras. Data inválida: " + dataHora);
                    }

                    List<StatusCarona> statusAtivos = List.of(StatusCarona.CRIADA, StatusCarona.EM_ANDAMENTO);

                    if (caronaRepository.existsByMotoristaIdAndDataHoraPartidaAndStatusIn(motoristaId, dataHora, statusAtivos)) {
                        throw new RegraDeNegocioException("Você já possui uma carona agendada ou em andamento para o dia/horário: " + dataHora);
                    }

                    return Carona.builder()
                            .motorista(motorista)
                            .veiculo(veiculo)
                            .origemDescricao(request.origem().descricao())
                            .origemLatitude(request.origem().latitude())
                            .origemLongitude(request.origem().longitude())
                            .destinoDescricao(request.destino().descricao())
                            .destinoLatitude(request.destino().latitude())
                            .destinoLongitude(request.destino().longitude())
                            .pontoEncontroDescricao(request.pontoEncontro())
                            .observacao(request.observacao())
                            .dataHoraPartida(dataHora)
                            .vagasTotais(request.quantidadeVagas())
                            .valorContribuicao(request.valorContribuicao())
                            .status(StatusCarona.CRIADA)
                            .build();
                })
                .toList();

        List<Carona> caronasSalvas = caronaRepository.saveAll(caronasParaSalvar);

        List<InteresseTrajeto> todosInteresses = interesseTrajetoRepository.findAll();

        caronasSalvas.forEach(carona -> {
            List<Long> usuarioIdsInteressados = todosInteresses.stream()
                    .filter(interesse -> !interesse.getUsuarioId().equals(motoristaId))
                    .filter(interesse -> interesse.getDestinoLatitude().compareTo(carona.getDestinoLatitude()) == 0
                            && interesse.getDestinoLongitude().compareTo(carona.getDestinoLongitude()) == 0)
                    .map(InteresseTrajeto::getUsuarioId)
                    .distinct()
                    .toList();

            if (!usuarioIdsInteressados.isEmpty()) {
                List<Usuario> usuariosInteressados = usuarioRepository.findAllById(usuarioIdsInteressados);

                usuariosInteressados.forEach(usuario -> {
                    notificacaoService.dispararNotificacaoSistemica(
                            usuario,
                            "Carona de seu Interesse Criada",
                            NotificacaoTemplates.novaCaronaDisponivel(carona),
                            TipoNotificacao.INTERESSE_TRAJETO
                    );
                });
            }
        });

        return caronasSalvas.stream()
                .map(c -> new CaronaResponseDTO(c.getId(), c.getStatus()))
                .toList();
    }

    @Transactional
    public CaronaResponseDTO atualizarObservacao(Long id, CaronaObservacaoRequestDTO request, Long motoristaId) {
        Carona carona = buscarCaronaParaAtualizacao(id);
        validarMotorista(carona, motoristaId);

        if (carona.getStatus() == StatusCarona.FINALIZADA || carona.getStatus() == StatusCarona.CANCELADA) {
            throw new EstadoInvalidoException(
                    "Não é possível atualizar a observação de uma carona com status " + carona.getStatus());
        }

        String observacao = request.observacao() != null ? request.observacao().trim() : null;
        carona.setObservacao(observacao != null && observacao.isEmpty() ? null : observacao);

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
                carona.getObservacao(),
                new MotoristaResumoDTO(
                        carona.getMotorista().getId(),
                        carona.getMotorista().getNome(),
                        carona.getMotorista().getLinkFoto()
                ),
                new VeiculoResumoDTO(carona.getVeiculo().getId(), carona.getVeiculo().getModelo(), carona.getVeiculo().getCor())
        );
    }

    @Transactional
    public CaronaResponseDTO atualizar(Long id, CaronaRequestDTO request, Long motoristaId) {
        Carona carona = buscarCaronaParaAtualizacao(id);
        validarMotorista(carona, motoristaId);

        if (request.datasHorasSaida() == null || request.datasHorasSaida().size() != 1) {
            throw new RegraDeNegocioException(
                    "A atualização de carona aceita exatamente uma data/hora de saída");
        }

        LocalDateTime novaDataHora = request.datasHorasSaida().getFirst();

        if (carona.getStatus() != StatusCarona.CRIADA) {
            throw new RegraDeNegocioException("Não é possível editar a carona após o início da viagem");
        }

        if (!novaDataHora.isAfter(LocalDateTime.now())) {
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

        validarValorContribuicao(request.origem(), request.destino(), BigDecimal.ZERO, request.valorContribuicao());

        carona.setOrigemDescricao(request.origem().descricao());
        carona.setOrigemLatitude(request.origem().latitude());
        carona.setOrigemLongitude(request.origem().longitude());
        carona.setDestinoDescricao(request.destino().descricao());
        carona.setDestinoLatitude(request.destino().latitude());
        carona.setDestinoLongitude(request.destino().longitude());
        carona.setPontoEncontroDescricao(request.pontoEncontro());
        carona.setDataHoraPartida(novaDataHora);
        carona.setObservacao(request.observacao());
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

            notificacaoService.dispararNotificacaoSistemica(
                    reserva.getUsuario(),
                    "Carona Cancelada",
                    NotificacaoTemplates.caronaCancelada(carona),
                    TipoNotificacao.CARONA_CANCELADA
            );
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

        List<ReservaCarona> reservas = reservaCaronaRepository.findByCaronaIdAndStatus(caronaId, StatusReserva.ACEITA);
        reservas.forEach(r -> {
            notificacaoService.dispararNotificacaoSistemica(
                    r.getUsuario(),
                    "Carona Iniciada",
                    NotificacaoTemplates.caronaIniciada(carona),
                    TipoNotificacao.CARONA_INICIADA
            );
        });
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

        reservas.forEach(r -> {
            r.setStatus(StatusReserva.CONCLUIDA);

            notificacaoService.dispararNotificacaoSistemica(
                    r.getUsuario(),
                    "Carona Finalizada",
                    NotificacaoTemplates.caronaFinalizada(carona),
                    TipoNotificacao.CARONA_FINALIZADA
            );

            notificacaoService.dispararNotificacaoSistemica(
                    r.getUsuario(),
                    "Avalie sua Viagem",
                    NotificacaoTemplates.solicitarAvaliacaoPassageiro(carona),
                    TipoNotificacao.NOTIFICACAO_AVALIACAO
            );
        });

        reservaCaronaRepository.saveAll(reservas);

        if (!reservas.isEmpty()) {
            notificacaoService.dispararNotificacaoSistemica(
                    carona.getMotorista(),
                    "Avalie seus Passageiros",
                    NotificacaoTemplates.solicitarAvaliacaoMotorista(),
                    TipoNotificacao.NOTIFICACAO_AVALIACAO
            );
        }
    }

    public long contaCaronasParticipadas(Long idUsuario){
        return caronaRepository.countByMotoristaIdAndStatus(idUsuario, StatusCarona.FINALIZADA) + reservaCaronaRepository.countByUsuarioIdAndStatus(idUsuario, StatusReserva.CONCLUIDA);
    }

    private Carona buscarCaronaParaAtualizacao(Long id) {
        return caronaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new CaronaNaoEncontradaException("Carona não encontrada: id=" + id));
    }

    private int contarPassageirosConfirmados(Long caronaId) {
        return reservaCaronaRepository.somarPassageirosPorCaronaEStatus(caronaId, StatusReserva.ACEITA);
    }

    private Carona buscarCarona(Long id) {
        return caronaRepository.findById(id)
                .orElseThrow(() -> new CaronaNaoEncontradaException("Carona não encontrada: id=" + id));
    }

    private void validarMotorista(Carona carona, Long motoristaId) {
        if (!carona.getMotorista().getId().equals(motoristaId)) {
            throw new AcessoNegadoException("O usuário não é o motorista desta carona");
        }
    }

    private BigDecimal calcularValorSugeridoPorVaga(EnderecoDTO origem, EnderecoDTO destino,
                                                 BigDecimal valorPedagios) {
        BigDecimal distanciaKm = GeoUtils.calcularDistanciaKm(
                origem.latitude(), origem.longitude(), destino.latitude(), destino.longitude());

        BigDecimal custoPorKm = precoCombustivelLitro.divide(consumoMedioKmLitro, 4, RoundingMode.HALF_UP);
        BigDecimal precoBase = distanciaKm.multiply(custoPorKm);

        BigDecimal pedagios = valorPedagios != null ? valorPedagios : BigDecimal.ZERO;

        return precoBase.add(pedagios).setScale(2, RoundingMode.HALF_UP);
    }

    private void validarValorContribuicao(EnderecoDTO origem, EnderecoDTO destino,
                                        BigDecimal valorPedagios, BigDecimal valorContribuicao) {
        BigDecimal valorSugerido = calcularValorSugeridoPorVaga(origem, destino, valorPedagios);

        BigDecimal valorMaximo = valorSugerido
                .multiply(BigDecimal.ONE.add(margemMaximaMotorista))
                .setScale(2, RoundingMode.HALF_UP);

        if (valorContribuicao.compareTo(valorMaximo) > 0) {
            throw new RegraDeNegocioException(
                    "O valor máximo permitido para este trajeto é R$ " + valorMaximo);
        }
    }
}
