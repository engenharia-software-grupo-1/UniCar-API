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
import com.unicar.dto.carona.ReservaStatusResponseDTO;
import com.unicar.enums.StatusCarona;
import com.unicar.enums.StatusReserva;
import com.unicar.enums.TipoNotificacao;
import com.unicar.exception.AcessoNegadoException;
import com.unicar.exception.CaronaNaoEncontradaException;
import com.unicar.exception.EstadoInvalidoException;
import com.unicar.exception.RegraDeNegocioException;
import com.unicar.exception.ReservaNaoEncontradaException;
import com.unicar.repository.CaronaRepository;
import com.unicar.repository.ReservaCaronaRepository;
import com.unicar.repository.UsuarioRepository;
import com.unicar.repository.chat.ChatRepository;
import com.unicar.service.NotificacaoService;
import com.unicar.util.GeoUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ReservaCaronaServiceTest {

    @Mock
    private ReservaCaronaRepository repository;

    @Mock
    private CaronaRepository caronaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private NotificacaoService notificacaoService;

    @InjectMocks
    private ReservaCaronaService service;

    private ReservaCarona reserva;
    private Usuario usuario;
    private Usuario motorista;
    private Carona carona;

    private final Long reservaId = 1L;
    private final Long usuarioId = 10L;
    private final Long outroUsuarioId = 20L;
    private final Long motoristaId = 30L;
    private final Long caronaId = 100L;

    private static final BigDecimal ORIGEM_LAT = new BigDecimal("-7.22000000");
    private static final BigDecimal ORIGEM_LON = new BigDecimal("-35.91000000");
    private static final BigDecimal DESTINO_LAT = new BigDecimal("-7.23000000");
    private static final BigDecimal DESTINO_LON = new BigDecimal("-35.87000000");
    private static final BigDecimal EMBARQUE_COMPATIVEL_LAT = new BigDecimal("-7.22500000");
    private static final BigDecimal EMBARQUE_COMPATIVEL_LON = new BigDecimal("-35.89000000");
    private static final BigDecimal EMBARQUE_INCOMPATIVEL_LAT = new BigDecimal("-3.71720000");
    private static final BigDecimal EMBARQUE_INCOMPATIVEL_LON = new BigDecimal("-38.54330000");

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "toleranciaTrajetoKm", new BigDecimal("3.0"));
        ReflectionTestUtils.setField(service, "prazoResposta", Duration.ofHours(24));
        ReflectionTestUtils.setField(service, "antecedenciaMinimaPartida", Duration.ofHours(1));

        usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setNome("Maria Oliveira");

        motorista = new Usuario();
        motorista.setId(motoristaId);
        motorista.setNome("João Motorista");

        carona = new Carona();
        carona.setId(caronaId);
        carona.setMotorista(motorista);
        carona.setStatus(StatusCarona.CRIADA);
        carona.setVagasTotais(4);
        carona.setValorContribuicao(new BigDecimal("10.00"));
        carona.setOrigemDescricao("Bodocongó");
        carona.setOrigemLatitude(ORIGEM_LAT);
        carona.setOrigemLongitude(ORIGEM_LON);
        carona.setDestinoDescricao("UFCG");
        carona.setDataHoraPartida(LocalDateTime.now().plusDays(1));
        carona.setDestinoLatitude(DESTINO_LAT);
        carona.setDestinoLongitude(DESTINO_LON);

        reserva = new ReservaCarona();
        reserva.setId(reservaId);
        reserva.setUsuario(usuario);
        reserva.setCarona(carona);
        reserva.setDataExpiracao(LocalDateTime.now().plusHours(2));
    }

    private ReservaRequestDTO criarRequest(BigDecimal embarqueLat, BigDecimal embarqueLon, int quantidadePassageiros) {
        return new ReservaRequestDTO(
                caronaId,
                quantidadePassageiros,
                new EnderecoDTO("Rua Aprígio Veloso", embarqueLat, embarqueLon)
        );
    }

    private BigDecimal calcularValorEsperado(BigDecimal embarqueLat, BigDecimal embarqueLon, int quantidadePassageiros) {
        BigDecimal distanciaTotal = GeoUtils.calcularDistanciaKm(ORIGEM_LAT, ORIGEM_LON, DESTINO_LAT, DESTINO_LON);
        BigDecimal distanciaEmbarqueDestino = GeoUtils.calcularDistanciaKm(embarqueLat, embarqueLon, DESTINO_LAT, DESTINO_LON);
        BigDecimal proporcao = distanciaEmbarqueDestino.divide(distanciaTotal, 10, RoundingMode.HALF_UP);
        return carona.getValorContribuicao()
                .multiply(proporcao)
                .multiply(BigDecimal.valueOf(quantidadePassageiros))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Nested
    @DisplayName("Solicitar reserva")
    class SolicitarReserva {

        @Test
        @DisplayName("Deve criar reserva PENDENTE com valor calculado corretamente")
        void deveCriarReserva() {
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));
            when(repository.somarPassageirosPorCaronaEStatus(caronaId, StatusReserva.ACEITA)).thenReturn(0);
            when(repository.existsByCarona_IdAndUsuario_IdAndStatusIn(eq(caronaId), eq(usuarioId), anyList()))
                    .thenReturn(false);
            when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
            when(repository.save(any(ReservaCarona.class))).thenAnswer(inv -> inv.getArgument(0));

            ReservaRequestDTO request = criarRequest(EMBARQUE_COMPATIVEL_LAT, EMBARQUE_COMPATIVEL_LON, 2);
            BigDecimal valorEsperado = calcularValorEsperado(EMBARQUE_COMPATIVEL_LAT, EMBARQUE_COMPATIVEL_LON, 2);

            ReservaResponseDTO response = service.solicitar(request, usuarioId);

            assertEquals(StatusReserva.PENDENTE, response.status());
            assertEquals(2, response.quantidadePassageiros());
            assertEquals(0, valorEsperado.compareTo(response.valorContribuicao()));

            ArgumentCaptor<ReservaCarona> captor = ArgumentCaptor.forClass(ReservaCarona.class);
            verify(repository).save(captor.capture());
            ReservaCarona salva = captor.getValue();
            assertEquals(carona, salva.getCarona());
            assertEquals(usuario, salva.getUsuario());
            assertEquals(StatusReserva.PENDENTE, salva.getStatus());
        }

        @Test
        @DisplayName("Deve conceder o prazo completo de resposta quando a carona estiver distante")
        void deveUsarPrazoCompletoDeResposta() {
            carona.setDataHoraPartida(LocalDateTime.now().plusDays(3));
            prepararSolicitacaoValida();

            service.solicitar(
                    criarRequest(EMBARQUE_COMPATIVEL_LAT, EMBARQUE_COMPATIVEL_LON, 1),
                    usuarioId
            );

            ArgumentCaptor<ReservaCarona> captor = ArgumentCaptor.forClass(ReservaCarona.class);
            verify(repository).save(captor.capture());
            ReservaCarona salva = captor.getValue();
            assertEquals(salva.getDataReserva().plusHours(24), salva.getDataExpiracao());
        }

        @Test
        @DisplayName("Deve limitar o prazo de resposta a uma hora antes da partida")
        void deveLimitarPrazoAntesDaPartida() {
            LocalDateTime dataPartida = LocalDateTime.now().plusHours(10);
            carona.setDataHoraPartida(dataPartida);
            prepararSolicitacaoValida();

            service.solicitar(
                    criarRequest(EMBARQUE_COMPATIVEL_LAT, EMBARQUE_COMPATIVEL_LON, 1),
                    usuarioId
            );

            ArgumentCaptor<ReservaCarona> captor = ArgumentCaptor.forClass(ReservaCarona.class);
            verify(repository).save(captor.capture());
            assertEquals(dataPartida.minusHours(1), captor.getValue().getDataExpiracao());
            verify(notificacaoService).dispararNotificacaoSistemica(
                    eq(motorista),
                    eq("Nova solicitação de reserva"),
                    contains(dataPartida.minusHours(1).format(
                            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")
                    )),
                    eq(TipoNotificacao.RESERVA_CRIADA)
            );
        }

        @Test
        @DisplayName("Não deve criar reserva quando o limite anterior à partida já foi atingido")
        void naoDeveCriarReservaComPrazoEncerrado() {
            carona.setDataHoraPartida(LocalDateTime.now().plusMinutes(30));
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));

            assertThrows(EstadoInvalidoException.class, () -> service.solicitar(
                    criarRequest(EMBARQUE_COMPATIVEL_LAT, EMBARQUE_COMPATIVEL_LON, 1),
                    usuarioId
            ));

            verify(repository, never()).save(any());
            verify(chatRepository, never()).save(any());
            verifyNoInteractions(notificacaoService);
        }

        private void prepararSolicitacaoValida() {
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));
            when(repository.somarPassageirosPorCaronaEStatus(caronaId, StatusReserva.ACEITA)).thenReturn(0);
            when(repository.existsByCarona_IdAndUsuario_IdAndStatusIn(eq(caronaId), eq(usuarioId), anyList()))
                    .thenReturn(false);
            when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
            when(repository.save(any(ReservaCarona.class))).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        @DisplayName("Não deve permitir motorista reservar a própria carona")
        void naoDevePermitirMotoristaReservarPropriaCarona() {
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));

            ReservaRequestDTO request = criarRequest(EMBARQUE_COMPATIVEL_LAT, EMBARQUE_COMPATIVEL_LON, 2);

            assertThrows(RegraDeNegocioException.class, () ->
                    service.solicitar(request, motoristaId));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Não deve reservar carona que não está CRIADA")
        void naoDeveReservarCaronaComStatusInvalido() {
            carona.setStatus(StatusCarona.EM_ANDAMENTO);
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));

            ReservaRequestDTO request = criarRequest(EMBARQUE_COMPATIVEL_LAT, EMBARQUE_COMPATIVEL_LON, 2);

            assertThrows(EstadoInvalidoException.class, () ->
                    service.solicitar(request, usuarioId));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Não deve reservar quando não há vagas suficientes")
        void naoDeveReservarSemVagasSuficientes() {
            carona.setVagasTotais(2);
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));
            when(repository.somarPassageirosPorCaronaEStatus(caronaId, StatusReserva.ACEITA)).thenReturn(1);

            ReservaRequestDTO request = criarRequest(EMBARQUE_COMPATIVEL_LAT, EMBARQUE_COMPATIVEL_LON, 2);

            assertThrows(RegraDeNegocioException.class, () ->
                    service.solicitar(request, usuarioId));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Não deve reservar quando uma única reserva aceita com múltiplos passageiros já ocupa todas as vagas")
        void naoDeveReservarQuandoReservaComMultiplosPassageirosOcupaTodasAsVagas() {
            carona.setVagasTotais(4);
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));
            // Uma única reserva ACEITA com 4 passageiros deve contar como 4 vagas ocupadas,
            // não como 1 (quantidade de linhas de reserva).
            when(repository.somarPassageirosPorCaronaEStatus(caronaId, StatusReserva.ACEITA)).thenReturn(4);

            ReservaRequestDTO request = criarRequest(EMBARQUE_COMPATIVEL_LAT, EMBARQUE_COMPATIVEL_LON, 1);

            assertThrows(RegraDeNegocioException.class, () ->
                    service.solicitar(request, usuarioId));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Não deve permitir reserva duplicada para a mesma carona")
        void naoDevePermitirReservaDuplicada() {
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));
            when(repository.somarPassageirosPorCaronaEStatus(caronaId, StatusReserva.ACEITA)).thenReturn(0);
            when(repository.existsByCarona_IdAndUsuario_IdAndStatusIn(eq(caronaId), eq(usuarioId), anyList()))
                    .thenReturn(true);

            ReservaRequestDTO request = criarRequest(EMBARQUE_COMPATIVEL_LAT, EMBARQUE_COMPATIVEL_LON, 2);

            assertThrows(RegraDeNegocioException.class, () ->
                    service.solicitar(request, usuarioId));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Não deve reservar quando local de embarque é incompatível com o trajeto")
        void naoDeveReservarComEmbarqueIncompativel() {
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));
            when(repository.somarPassageirosPorCaronaEStatus(caronaId, StatusReserva.ACEITA)).thenReturn(0);
            when(repository.existsByCarona_IdAndUsuario_IdAndStatusIn(eq(caronaId), eq(usuarioId), anyList()))
                    .thenReturn(false);

            ReservaRequestDTO request = criarRequest(EMBARQUE_INCOMPATIVEL_LAT, EMBARQUE_INCOMPATIVEL_LON, 2);

            assertThrows(RegraDeNegocioException.class, () ->
                    service.solicitar(request, usuarioId));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar erro quando carona não existir")
        void deveLancarErroQuandoCaronaNaoExistir() {
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.empty());

            ReservaRequestDTO request = criarRequest(EMBARQUE_COMPATIVEL_LAT, EMBARQUE_COMPATIVEL_LON, 2);

            assertThrows(CaronaNaoEncontradaException.class, () ->
                    service.solicitar(request, usuarioId));
        }

        @Test
        @DisplayName("Deve lançar erro quando usuário autenticado não for encontrado")
        void deveLancarErroQuandoUsuarioNaoForEncontrado() {
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));
            when(repository.somarPassageirosPorCaronaEStatus(caronaId, StatusReserva.ACEITA)).thenReturn(0);
            when(repository.existsByCarona_IdAndUsuario_IdAndStatusIn(eq(caronaId), eq(usuarioId), anyList()))
                    .thenReturn(false);
            when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

            ReservaRequestDTO request = criarRequest(EMBARQUE_COMPATIVEL_LAT, EMBARQUE_COMPATIVEL_LON, 2);

            assertThrows(AcessoNegadoException.class, () -> service.solicitar(request, usuarioId));

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Simular reserva")
    class SimularReserva {

        @Test
        @DisplayName("Deve calcular valor sem criar reserva")
        void deveSimularSemCriarReserva() {
            when(caronaRepository.findById(caronaId)).thenReturn(Optional.of(carona));

            ReservaRequestDTO request = criarRequest(EMBARQUE_COMPATIVEL_LAT, EMBARQUE_COMPATIVEL_LON, 2);
            BigDecimal valorEsperado = calcularValorEsperado(EMBARQUE_COMPATIVEL_LAT, EMBARQUE_COMPATIVEL_LON, 2);

            ReservaSimulacaoResponseDTO response = service.simular(request);

            assertEquals(0, valorEsperado.compareTo(response.valorContribuicaoEstimado()));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar erro quando carona não existir")
        void deveLancarErroQuandoCaronaNaoExistir() {
            when(caronaRepository.findById(caronaId)).thenReturn(Optional.empty());

            ReservaRequestDTO request = criarRequest(EMBARQUE_COMPATIVEL_LAT, EMBARQUE_COMPATIVEL_LON, 2);

            assertThrows(CaronaNaoEncontradaException.class, () -> service.simular(request));
        }

        @Test
        @DisplayName("Deve lançar erro quando origem e destino da carona forem o mesmo ponto")
        void deveLancarErroQuandoDistanciaTotalForZero() {
            carona.setDestinoLatitude(ORIGEM_LAT);
            carona.setDestinoLongitude(ORIGEM_LON);
            when(caronaRepository.findById(caronaId)).thenReturn(Optional.of(carona));

            ReservaRequestDTO request = criarRequest(EMBARQUE_COMPATIVEL_LAT, EMBARQUE_COMPATIVEL_LON, 2);

            assertThrows(RegraDeNegocioException.class, () -> service.simular(request));
        }
    }

    @Nested
    @DisplayName("Listar reservas enviadas")
    class ListarEnviadas {

        @Test
        @DisplayName("Deve listar reservas do usuário autenticado")
        void deveListarReservasEnviadas() {
            reserva.setCarona(carona);
            reserva.setStatus(StatusReserva.PENDENTE);
            reserva.setQuantidadePassageiros(2);
            reserva.setValorContribuicao(new BigDecimal("8.00"));

            when(repository.findByUsuario_Id(usuarioId)).thenReturn(List.of(reserva));

            List<ReservaEnviadaResponseDTO> resultado = service.listarEnviadas(usuarioId);

            assertEquals(1, resultado.size());
            assertEquals(reservaId, resultado.get(0).id());
            assertEquals(caronaId, resultado.get(0).carona().id());
        }
    }

    @Nested
    @DisplayName("Listar reservas recebidas")
    class ListarRecebidas {

        @Test
        @DisplayName("Deve listar reservas das caronas do motorista autenticado")
        void deveListarReservasRecebidas() {
            reserva.setCarona(carona);
            reserva.setStatus(StatusReserva.PENDENTE);
            reserva.setQuantidadePassageiros(2);
            reserva.setValorContribuicao(new BigDecimal("8.00"));
            reserva.setOrigemEmbarqueDescricao("Rua Aprígio Veloso");
            reserva.setOrigemEmbarqueLatitude(EMBARQUE_COMPATIVEL_LAT);
            reserva.setOrigemEmbarqueLongitude(EMBARQUE_COMPATIVEL_LON);

            when(repository.findByCarona_Motorista_Id(motoristaId)).thenReturn(List.of(reserva));

            List<ReservaRecebidaResponseDTO> resultado = service.listarRecebidas(motoristaId);

            assertEquals(1, resultado.size());
            assertEquals(usuarioId, resultado.get(0).usuario().id());
        }
    }

    @Nested
    @DisplayName("Buscar detalhe da reserva")
    class BuscarDetalhe {

        @BeforeEach
        void setupReserva() {
            reserva.setCarona(carona);
            reserva.setStatus(StatusReserva.PENDENTE);
            reserva.setQuantidadePassageiros(2);
            reserva.setValorContribuicao(new BigDecimal("8.00"));
            reserva.setOrigemEmbarqueDescricao("Rua Aprígio Veloso");
            reserva.setOrigemEmbarqueLatitude(EMBARQUE_COMPATIVEL_LAT);
            reserva.setOrigemEmbarqueLongitude(EMBARQUE_COMPATIVEL_LON);
        }

        @Test
        @DisplayName("Deve permitir que o passageiro consulte a reserva")
        void devePermitirPassageiroConsultar() {
            when(repository.findById(reservaId)).thenReturn(Optional.of(reserva));

            ReservaDetalheResponseDTO resultado = service.buscarDetalhe(reservaId, usuarioId);

            assertEquals(reservaId, resultado.id());
        }

        @Test
        @DisplayName("Deve permitir que o motorista consulte a reserva")
        void devePermitirMotoristaConsultar() {
            when(repository.findById(reservaId)).thenReturn(Optional.of(reserva));

            ReservaDetalheResponseDTO resultado = service.buscarDetalhe(reservaId, motoristaId);

            assertEquals(reservaId, resultado.id());
        }

        @Test
        @DisplayName("Não deve permitir que outro usuário consulte a reserva")
        void naoDevePermitirOutroUsuarioConsultar() {
            when(repository.findById(reservaId)).thenReturn(Optional.of(reserva));

            assertThrows(AcessoNegadoException.class, () ->
                    service.buscarDetalhe(reservaId, outroUsuarioId));
        }
    }

    @Nested
    @DisplayName("Aceitar reserva")
    class AceitarReserva {

        @BeforeEach
        void setupReserva() {
            reserva.setStatus(StatusReserva.PENDENTE);
            reserva.setQuantidadePassageiros(2);
        }

        @Test
        @DisplayName("Deve aceitar reserva pendente quando há vagas suficientes")
        void deveAceitarReserva() {
            when(repository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));
            when(repository.somarPassageirosPorCaronaEStatus(caronaId, StatusReserva.ACEITA)).thenReturn(0);
            when(repository.save(any(ReservaCarona.class))).thenAnswer(inv -> inv.getArgument(0));

            ReservaStatusResponseDTO response = service.aceitar(reservaId, motoristaId);

            assertEquals(StatusReserva.ACEITA, response.status());
            assertEquals(StatusReserva.ACEITA, reserva.getStatus());
            assertNotNull(reserva.getDataResposta());
        }

        @Test
        @DisplayName("Não deve aceitar se usuário não for o motorista da carona")
        void naoDeveAceitarSeNaoForMotorista() {
            when(repository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));

            assertThrows(AcessoNegadoException.class, () ->
                    service.aceitar(reservaId, outroUsuarioId));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Não deve aceitar reserva que não está PENDENTE")
        void naoDeveAceitarReservaComStatusInvalido() {
            reserva.setStatus(StatusReserva.ACEITA);
            when(repository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));

            assertThrows(EstadoInvalidoException.class, () ->
                    service.aceitar(reservaId, motoristaId));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Não deve aceitar reserva pendente depois do prazo de resposta")
        void naoDeveAceitarReservaExpirada() {
            reserva.setDataExpiracao(LocalDateTime.now().minusSeconds(1));
            when(repository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));

            assertThrows(EstadoInvalidoException.class, () ->
                    service.aceitar(reservaId, motoristaId));

            verify(caronaRepository, never()).findByIdForUpdate(any());
            verify(repository, never()).save(any());
            verifyNoInteractions(notificacaoService);
        }

        @Test
        @DisplayName("Não deve aceitar quando não há vagas suficientes")
        void naoDeveAceitarSemVagasSuficientes() {
            carona.setVagasTotais(2);
            when(repository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));
            when(repository.somarPassageirosPorCaronaEStatus(caronaId, StatusReserva.ACEITA)).thenReturn(1);

            assertThrows(RegraDeNegocioException.class, () ->
                    service.aceitar(reservaId, motoristaId));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Não deve aceitar quando uma única reserva aceita com múltiplos passageiros já ocupa todas as vagas")
        void naoDeveAceitarQuandoReservaComMultiplosPassageirosOcupaTodasAsVagas() {
            carona.setVagasTotais(4);
            reserva.setQuantidadePassageiros(1);
            when(repository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));
            // Outra reserva ACEITA de 4 passageiros já ocupa a carona inteira, mesmo sendo
            // uma única linha na tabela de reservas.
            when(repository.somarPassageirosPorCaronaEStatus(caronaId, StatusReserva.ACEITA)).thenReturn(4);

            assertThrows(RegraDeNegocioException.class, () ->
                    service.aceitar(reservaId, motoristaId));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar erro quando reserva não existir")
        void deveLancarErroQuandoReservaNaoExistir() {
            when(repository.findByIdForUpdate(reservaId)).thenReturn(Optional.empty());

            assertThrows(ReservaNaoEncontradaException.class, () ->
                    service.aceitar(reservaId, motoristaId));
        }
    }

    @Nested
    @DisplayName("Recusar reserva")
    class RecusarReserva {

        @BeforeEach
        void setupReserva() {
            reserva.setStatus(StatusReserva.PENDENTE);
        }

        @Test
        @DisplayName("Deve recusar reserva pendente")
        void deveRecusarReserva() {
            when(repository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
            when(repository.save(any(ReservaCarona.class))).thenAnswer(inv -> inv.getArgument(0));

            ReservaStatusResponseDTO response = service.recusar(reservaId, motoristaId);

            assertEquals(StatusReserva.RECUSADA, response.status());
            assertEquals(StatusReserva.RECUSADA, reserva.getStatus());
            assertNotNull(reserva.getDataResposta());
        }

        @Test
        @DisplayName("Não deve recusar reserva pendente depois do prazo de resposta")
        void naoDeveRecusarReservaExpirada() {
            reserva.setDataExpiracao(LocalDateTime.now().minusSeconds(1));
            when(repository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));

            assertThrows(EstadoInvalidoException.class, () ->
                    service.recusar(reservaId, motoristaId));

            verify(repository, never()).save(any());
            verifyNoInteractions(notificacaoService);
        }

        @Test
        @DisplayName("Não deve recusar se usuário não for o motorista da carona")
        void naoDeveRecusarSeNaoForMotorista() {
            when(repository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));

            assertThrows(AcessoNegadoException.class, () ->
                    service.recusar(reservaId, outroUsuarioId));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Não deve recusar reserva que não está PENDENTE")
        void naoDeveRecusarReservaComStatusInvalido() {
            reserva.setStatus(StatusReserva.CANCELADA);
            when(repository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));

            assertThrows(EstadoInvalidoException.class, () ->
                    service.recusar(reservaId, motoristaId));

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Cancelar reserva")
    class CancelarReserva {

        @Test
        @DisplayName("Deve permitir que o passageiro cancele reserva PENDENTE")
        void devePermitirPassageiroCancelarPendente() {
            reserva.setStatus(StatusReserva.PENDENTE);
            when(repository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
            when(repository.save(any(ReservaCarona.class))).thenAnswer(inv -> inv.getArgument(0));

            ReservaStatusResponseDTO response = service.cancelar(reservaId, usuarioId);

            assertEquals(StatusReserva.CANCELADA, response.status());
        }

        @Test
        @DisplayName("Deve permitir que o passageiro cancele reserva ACEITA")
        void devePermitirPassageiroCancelarAceita() {
            reserva.setStatus(StatusReserva.ACEITA);
            when(repository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
            when(repository.save(any(ReservaCarona.class))).thenAnswer(inv -> inv.getArgument(0));

            ReservaStatusResponseDTO response = service.cancelar(reservaId, usuarioId);

            assertEquals(StatusReserva.CANCELADA, response.status());
        }

        @Test
        @DisplayName("Deve permitir que o motorista cancele reserva ACEITA")
        void devePermitirMotoristaCancelarAceita() {
            reserva.setStatus(StatusReserva.ACEITA);
            when(repository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
            when(repository.save(any(ReservaCarona.class))).thenAnswer(inv -> inv.getArgument(0));

            ReservaStatusResponseDTO response = service.cancelar(reservaId, motoristaId);

            assertEquals(StatusReserva.CANCELADA, response.status());
        }

        @Test
        @DisplayName("Não deve permitir que o motorista cancele reserva PENDENTE")
        void naoDevePermitirMotoristaCancelarPendente() {
            reserva.setStatus(StatusReserva.PENDENTE);
            when(repository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));

            assertThrows(EstadoInvalidoException.class, () ->
                    service.cancelar(reservaId, motoristaId));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Não deve permitir cancelar reserva finalizada")
        void naoDevePermitirCancelarReservaFinalizada() {
            reserva.setStatus(StatusReserva.CONCLUIDA);
            when(repository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));

            assertThrows(EstadoInvalidoException.class, () ->
                    service.cancelar(reservaId, usuarioId));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Não deve permitir cancelar reserva já cancelada")
        void naoDevePermitirCancelarReservaJaCancelada() {
            reserva.setStatus(StatusReserva.CANCELADA);
            when(repository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));

            assertThrows(EstadoInvalidoException.class, () ->
                    service.cancelar(reservaId, usuarioId));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Não deve permitir que terceiro cancele a reserva")
        void naoDevePermitirTerceiroCancelar() {
            reserva.setStatus(StatusReserva.PENDENTE);
            when(repository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));

            assertThrows(AcessoNegadoException.class, () ->
                    service.cancelar(reservaId, outroUsuarioId));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar erro quando reserva não existir")
        void deveLancarErroQuandoReservaNaoExistir() {
            when(repository.findByIdForUpdate(reservaId)).thenReturn(Optional.empty());

            assertThrows(ReservaNaoEncontradaException.class, () ->
                    service.cancelar(reservaId, usuarioId));
        }
    }

    @Nested
    @DisplayName("Remover reserva")
    class RemoverReserva {

        @Test
        @DisplayName("Não deve remover reserva pendente")
        void naoDeveRemoverReservaPendente() {


            reserva.setStatus(StatusReserva.PENDENTE);


            when(repository.findByIdForUpdate(reservaId))
                    .thenReturn(Optional.of(reserva));


            assertThrows(
                    EstadoInvalidoException.class,
                    () -> service.removerReservaPassageiro(
                            reservaId,
                            motoristaId
                    )
            );

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve remover reserva aceita")
        void deveRemoverReservaAceita() {


            reserva.setStatus(StatusReserva.ACEITA);


            when(repository.findByIdForUpdate(reservaId))
                    .thenReturn(Optional.of(reserva));


            service.removerReservaPassageiro(
                    reservaId,
                    motoristaId
            );


            assertEquals(
                    StatusReserva.CANCELADA,
                    reserva.getStatus()
            );


            verify(repository)
                    .save(reserva);
        }

        @Test
        @DisplayName("Não deve remover reserva com status inválido")
        void naoDeveRemoverReservaComStatusInvalido() {


            reserva.setStatus(StatusReserva.CANCELADA);


            when(repository.findByIdForUpdate(reservaId))
                    .thenReturn(Optional.of(reserva));

            assertThrows(
                    EstadoInvalidoException.class,
                    () ->
                            service.removerReservaPassageiro(
                                    reservaId,
                                    motoristaId
                            )
            );


            verify(repository, never())
                    .save(any());
        }

        @Test
        @DisplayName("Não deve remover reserva de outro usuário")
        void naoDeveRemoverReservaDeOutroUsuario() {


            reserva.setStatus(StatusReserva.ACEITA);


            when(repository.findByIdForUpdate(reservaId))
                    .thenReturn(Optional.of(reserva));


            assertThrows(
                    AcessoNegadoException.class,
                    () ->
                            service.removerReservaPassageiro(
                                    reservaId,
                                    outroUsuarioId
                            )
            );


            verify(repository, never())
                    .save(any());
        }
    }

    @Nested
    @DisplayName("Buscar reserva")
    class BuscarReserva {
        @Test
        @DisplayName("Deve buscar reserva existente")
        void deveBuscarReserva() {
            when(repository.findById(reservaId))
                    .thenReturn(Optional.of(reserva));

            ReservaCarona resultado =
                    service.buscarReserva(reservaId);

            assertEquals(
                    reserva,
                    resultado
            );

            verify(repository)
                    .findById(reservaId);
        }

        @Test
        @DisplayName("Deve lançar erro quando reserva não existir")
        void deveLancarErroQuandoReservaNaoExistir() {

            when(repository.findById(reservaId))
                    .thenReturn(Optional.empty());

            assertThrows(ReservaNaoEncontradaException.class, () ->service.buscarReserva(reservaId));
        }
    }
}
