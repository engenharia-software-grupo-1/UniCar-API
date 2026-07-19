package com.unicar.service.carona;
import com.unicar.domain.Carona;
import com.unicar.domain.ReservaCarona;
import com.unicar.domain.Usuario;
import com.unicar.domain.Veiculo;
import com.unicar.dto.carona.CaronaDetalheResponseDTO;
import com.unicar.dto.carona.CaronaListItemResponseDTO;
import com.unicar.dto.carona.CaronaObservacaoRequestDTO;
import com.unicar.dto.carona.CaronaRequestDTO;
import com.unicar.dto.carona.CaronaResponseDTO;
import com.unicar.dto.carona.EnderecoDTO;
import com.unicar.dto.carona.PassageiroResponseDTO;
import com.unicar.enums.StatusCarona;
import com.unicar.enums.StatusReserva;
import com.unicar.exception.AcessoNegadoException;
import com.unicar.exception.CaronaNaoEncontradaException;
import com.unicar.exception.EstadoInvalidoException;
import com.unicar.exception.RegraDeNegocioException;
import com.unicar.exception.VeiculoNaoEncontradoException;
import com.unicar.repository.CaronaRepository;
import com.unicar.repository.InteresseTrajetoRepository;
import com.unicar.repository.ReservaCaronaRepository;
import com.unicar.repository.UsuarioRepository;
import com.unicar.repository.VeiculoRepository;
import com.unicar.service.NotificacaoService;
import com.unicar.util.GeoUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class CaronaServiceTest {
    @Mock
    private CaronaRepository caronaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private VeiculoRepository veiculoRepository;
    @Mock
    private ReservaCaronaRepository reservaCaronaRepository;
    @Mock
    private InteresseTrajetoRepository interesseTrajetoRepository;
    @Mock
    private NotificacaoService notificacaoService;
    @InjectMocks
    private CaronaService caronaService;
    private final Long caronaId = 1L;
    private final Long motoristaId = 10L;
    private final Long outroUsuarioId = 20L;
    private final Long veiculoId = 50L;

    private static final BigDecimal ORIGEM_LAT = new BigDecimal("-7.22000000");
    private static final BigDecimal ORIGEM_LON = new BigDecimal("-35.91000000");
    private static final BigDecimal DESTINO_LAT = new BigDecimal("-7.23000000");
    private static final BigDecimal DESTINO_LON = new BigDecimal("-35.87000000");

    private Carona carona;
    private Usuario motorista;
    private Veiculo veiculo;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(caronaService, "fatorValorPorKm", new BigDecimal("1.00"));

        motorista = new Usuario();
        motorista.setId(motoristaId);

        veiculo = new Veiculo();
        veiculo.setId(veiculoId);
        veiculo.setUsuario(motorista);

        carona = new Carona();
        carona.setId(caronaId);
        carona.setMotorista(motorista);
        carona.setVeiculo(veiculo);
        carona.setStatus(StatusCarona.CRIADA);
        carona.setVagasTotais(4);
        carona.setValorContribuicao(new BigDecimal("4.00"));
        carona.setOrigemDescricao("Bodocongó");
        carona.setOrigemLatitude(ORIGEM_LAT);
        carona.setOrigemLongitude(ORIGEM_LON);
        carona.setDestinoDescricao("UFCG");
        carona.setDestinoLatitude(DESTINO_LAT);
        carona.setDestinoLongitude(DESTINO_LON);
        carona.setPontoEncontroDescricao("Portaria");
        carona.setDataHoraPartida(LocalDateTime.now().plusDays(1));
    }

    private CaronaRequestDTO criarRequestValido(LocalDateTime... datas) {
        return new CaronaRequestDTO(
                veiculoId,
                new EnderecoDTO("Bodocongó", ORIGEM_LAT, ORIGEM_LON),
                new EnderecoDTO("UFCG", DESTINO_LAT, DESTINO_LON),
                "Portaria",
                List.of(datas),
                4,
                new BigDecimal("4.00"),
                "Observação"
        );
    }

    @Nested
    @DisplayName("Criar carona")
    class CriarCarona {

        @Test
        @DisplayName("Deve criar carona com sucesso")
        void deveCriarCarona() {
            LocalDateTime dataFutura = LocalDateTime.now().plusDays(2);
            when(usuarioRepository.findByIdForUpdate(motoristaId)).thenReturn(Optional.of(motorista));
            when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
            when(caronaRepository.existsByMotorista_IdAndStatus(motoristaId, StatusCarona.EM_ANDAMENTO)).thenReturn(false);
            when(caronaRepository.existsByMotoristaIdAndDataHoraPartidaAndStatusIn(eq(motoristaId), eq(dataFutura), anyList()))
                    .thenReturn(false);
            when(caronaRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<CaronaResponseDTO> resultado = caronaService.criar(criarRequestValido(dataFutura), motoristaId);

            assertEquals(1, resultado.size());
            assertEquals(StatusCarona.CRIADA, resultado.get(0).status());
            verify(caronaRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Deve criar uma carona para cada data informada (recorrência)")
        void deveCriarUmaCaronaPorData() {
            when(usuarioRepository.findByIdForUpdate(motoristaId)).thenReturn(Optional.of(motorista));
            when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
            when(caronaRepository.existsByMotorista_IdAndStatus(motoristaId, StatusCarona.EM_ANDAMENTO)).thenReturn(false);
            when(caronaRepository.existsByMotoristaIdAndDataHoraPartidaAndStatusIn(eq(motoristaId), any(), anyList()))
                    .thenReturn(false);
            when(caronaRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<CaronaResponseDTO> resultado = caronaService.criar(
                    criarRequestValido(LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(3)),
                    motoristaId);

            assertEquals(2, resultado.size());
        }

        @Test
        @DisplayName("Não deve criar quando motorista não existir")
        void naoDeveCriarQuandoMotoristaNaoExistir() {
            when(usuarioRepository.findByIdForUpdate(motoristaId)).thenReturn(Optional.empty());

            assertThrows(AcessoNegadoException.class, () ->
                    caronaService.criar(criarRequestValido(LocalDateTime.now().plusDays(1)), motoristaId));

            verify(caronaRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Não deve criar quando veículo não existir")
        void naoDeveCriarQuandoVeiculoNaoExistir() {
            when(usuarioRepository.findByIdForUpdate(motoristaId)).thenReturn(Optional.of(motorista));
            when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.empty());

            assertThrows(VeiculoNaoEncontradoException.class, () ->
                    caronaService.criar(criarRequestValido(LocalDateTime.now().plusDays(1)), motoristaId));
        }

        @Test
        @DisplayName("Não deve criar quando veículo não pertence ao motorista")
        void naoDeveCriarQuandoVeiculoNaoPertenceAoMotorista() {
            Usuario outroDono = new Usuario();
            outroDono.setId(outroUsuarioId);
            veiculo.setUsuario(outroDono);

            when(usuarioRepository.findByIdForUpdate(motoristaId)).thenReturn(Optional.of(motorista));
            when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));

            assertThrows(AcessoNegadoException.class, () ->
                    caronaService.criar(criarRequestValido(LocalDateTime.now().plusDays(1)), motoristaId));
        }

        @Test
        @DisplayName("Não deve criar quando quantidade de vagas for nula")
        void naoDeveCriarQuandoQuantidadeVagasForNula() {
            when(usuarioRepository.findByIdForUpdate(motoristaId)).thenReturn(Optional.of(motorista));
            when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));

            CaronaRequestDTO request = new CaronaRequestDTO(
                    veiculoId, new EnderecoDTO("Bodocongó", ORIGEM_LAT, ORIGEM_LON),
                    new EnderecoDTO("UFCG", DESTINO_LAT, DESTINO_LON), "Portaria",
                    List.of(LocalDateTime.now().plusDays(1)), null, new BigDecimal("4.00"), null);

            assertThrows(RegraDeNegocioException.class, () -> caronaService.criar(request, motoristaId));
        }

        @Test
        @DisplayName("Não deve criar quando motorista já tiver carona em andamento")
        void naoDeveCriarQuandoMotoristaTemCaronaEmAndamento() {
            when(usuarioRepository.findByIdForUpdate(motoristaId)).thenReturn(Optional.of(motorista));
            when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
            when(caronaRepository.existsByMotorista_IdAndStatus(motoristaId, StatusCarona.EM_ANDAMENTO)).thenReturn(true);

            assertThrows(RegraDeNegocioException.class, () ->
                    caronaService.criar(criarRequestValido(LocalDateTime.now().plusDays(1)), motoristaId));
        }

        @Test
        @DisplayName("Não deve criar quando valor de contribuição ultrapassa o limite")
        void naoDeveCriarQuandoValorUltrapassaLimite() {
            when(usuarioRepository.findByIdForUpdate(motoristaId)).thenReturn(Optional.of(motorista));
            when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
            when(caronaRepository.existsByMotorista_IdAndStatus(motoristaId, StatusCarona.EM_ANDAMENTO)).thenReturn(false);

            CaronaRequestDTO request = new CaronaRequestDTO(
                    veiculoId, new EnderecoDTO("Bodocongó", ORIGEM_LAT, ORIGEM_LON),
                    new EnderecoDTO("UFCG", DESTINO_LAT, DESTINO_LON), "Portaria",
                    List.of(LocalDateTime.now().plusDays(1)), 4, new BigDecimal("100.00"), null);

            assertThrows(RegraDeNegocioException.class, () -> caronaService.criar(request, motoristaId));
        }

        @Test
        @DisplayName("Não deve criar quando a data informada não for futura")
        void naoDeveCriarQuandoDataNaoForFutura() {
            when(usuarioRepository.findByIdForUpdate(motoristaId)).thenReturn(Optional.of(motorista));
            when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
            when(caronaRepository.existsByMotorista_IdAndStatus(motoristaId, StatusCarona.EM_ANDAMENTO)).thenReturn(false);

            assertThrows(RegraDeNegocioException.class, () ->
                    caronaService.criar(criarRequestValido(LocalDateTime.now().minusDays(1)), motoristaId));
        }

        @Test
        @DisplayName("Não deve criar quando já existir carona agendada na mesma data/hora")
        void naoDeveCriarQuandoJaExisteCaronaNaMesmaData() {
            LocalDateTime dataFutura = LocalDateTime.now().plusDays(2);
            when(usuarioRepository.findByIdForUpdate(motoristaId)).thenReturn(Optional.of(motorista));
            when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
            when(caronaRepository.existsByMotorista_IdAndStatus(motoristaId, StatusCarona.EM_ANDAMENTO)).thenReturn(false);
            when(caronaRepository.existsByMotoristaIdAndDataHoraPartidaAndStatusIn(eq(motoristaId), eq(dataFutura), anyList()))
                    .thenReturn(true);

            assertThrows(RegraDeNegocioException.class, () ->
                    caronaService.criar(criarRequestValido(dataFutura), motoristaId));
        }

        @ParameterizedTest(name = "vagas={0} -> aceita={1}")
        @CsvSource({
                "-1, false",
                "0, false",
                "1, true",
                "4, true"
        })
        @DisplayName("Valor limite: quantidade de vagas na criação da carona")
        void valorLimiteQuantidadeVagas(int quantidadeVagas, boolean deveSerAceita) {
            when(usuarioRepository.findByIdForUpdate(motoristaId)).thenReturn(Optional.of(motorista));
            when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
            if (deveSerAceita) {
                when(caronaRepository.existsByMotorista_IdAndStatus(motoristaId, StatusCarona.EM_ANDAMENTO)).thenReturn(false);
                when(caronaRepository.existsByMotoristaIdAndDataHoraPartidaAndStatusIn(any(), any(), anyList()))
                        .thenReturn(false);
                when(caronaRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
            }

            CaronaRequestDTO request = new CaronaRequestDTO(
                    veiculoId, new EnderecoDTO("Bodocongó", ORIGEM_LAT, ORIGEM_LON),
                    new EnderecoDTO("UFCG", DESTINO_LAT, DESTINO_LON), "Portaria",
                    List.of(LocalDateTime.now().plusDays(1)), quantidadeVagas, new BigDecimal("4.00"), null);

            if (deveSerAceita) {
                assertDoesNotThrow(() -> caronaService.criar(request, motoristaId));
            } else {
                assertThrows(RegraDeNegocioException.class, () -> caronaService.criar(request, motoristaId));
            }
        }
    }

    @Nested
    @DisplayName("Atualizar observação")
    class AtualizarObservacao {

        @Test
        @DisplayName("Deve atualizar a observação com sucesso")
        void deveAtualizarObservacao() {
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));
            when(caronaRepository.save(any(Carona.class))).thenAnswer(inv -> inv.getArgument(0));

            CaronaResponseDTO response = caronaService.atualizarObservacao(
                    caronaId, new CaronaObservacaoRequestDTO("Nova observação"), motoristaId);

            assertEquals("Nova observação", carona.getObservacao());
            assertEquals(caronaId, response.id());
        }

        @Test
        @DisplayName("Deve limpar a observação quando string vazia após trim")
        void deveLimparObservacaoQuandoVazia() {
            carona.setObservacao("Antiga");
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));
            when(caronaRepository.save(any(Carona.class))).thenAnswer(inv -> inv.getArgument(0));

            caronaService.atualizarObservacao(caronaId, new CaronaObservacaoRequestDTO("   "), motoristaId);

            assertNull(carona.getObservacao());
        }

        @Test
        @DisplayName("Deve manter observação nula quando não informada")
        void deveManterObservacaoNulaQuandoNaoInformada() {
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));
            when(caronaRepository.save(any(Carona.class))).thenAnswer(inv -> inv.getArgument(0));

            caronaService.atualizarObservacao(caronaId, new CaronaObservacaoRequestDTO(null), motoristaId);

            assertNull(carona.getObservacao());
        }

        @Test
        @DisplayName("Não deve atualizar se usuário não for o motorista")
        void naoDeveAtualizarSeNaoForMotorista() {
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));

            assertThrows(AcessoNegadoException.class, () ->
                    caronaService.atualizarObservacao(caronaId, new CaronaObservacaoRequestDTO("x"), outroUsuarioId));
        }

        @ParameterizedTest
        @EnumSource(value = StatusCarona.class, names = {"FINALIZADA", "CANCELADA"})
        @DisplayName("Não deve atualizar observação de carona finalizada ou cancelada")
        void naoDeveAtualizarObservacaoComStatusInvalido(StatusCarona status) {
            carona.setStatus(status);
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));

            assertThrows(EstadoInvalidoException.class, () ->
                    caronaService.atualizarObservacao(caronaId, new CaronaObservacaoRequestDTO("x"), motoristaId));
        }
    }

    @Nested
    @DisplayName("Listar minhas caronas")
    class ListarMinhas {

        @Test
        @DisplayName("Deve listar caronas do motorista")
        void deveListarCaronasDoMotorista() {
            when(caronaRepository.findByMotorista_Id(motoristaId)).thenReturn(List.of(carona));

            List<CaronaListItemResponseDTO> resultado = caronaService.listarMinhas(motoristaId);

            assertEquals(1, resultado.size());
            assertEquals(caronaId, resultado.get(0).id());
            assertEquals(StatusCarona.CRIADA, resultado.get(0).status());
        }
    }

    @Nested
    @DisplayName("Buscar carona por id")
    class BuscarPorId {

        @Test
        @DisplayName("Deve retornar detalhes com vagas disponíveis calculadas corretamente")
        void deveBuscarDetalhes() {
            when(caronaRepository.findById(caronaId)).thenReturn(Optional.of(carona));
            when(reservaCaronaRepository.somarPassageirosPorCaronaEStatus(caronaId, StatusReserva.ACEITA)).thenReturn(1);

            CaronaDetalheResponseDTO resultado = caronaService.buscarPorId(caronaId);

            assertEquals(caronaId, resultado.id());
            assertEquals(3, resultado.vagasDisponiveis());
        }

        @Test
        @DisplayName("Deve lançar erro quando carona não existir")
        void deveLancarErroQuandoNaoExistir() {
            when(caronaRepository.findById(caronaId)).thenReturn(Optional.empty());

            assertThrows(CaronaNaoEncontradaException.class, () -> caronaService.buscarPorId(caronaId));
        }
    }

    @Nested
    @DisplayName("Atualizar carona")
    class AtualizarCarona {

        @Test
        @DisplayName("Deve atualizar carona com sucesso")
        void deveAtualizarCarona() {
            LocalDateTime novaData = LocalDateTime.now().plusDays(3);
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));
            when(reservaCaronaRepository.somarPassageirosPorCaronaEStatus(caronaId, StatusReserva.ACEITA)).thenReturn(0);
            when(caronaRepository.save(any(Carona.class))).thenAnswer(inv -> inv.getArgument(0));

            CaronaResponseDTO response = caronaService.atualizar(caronaId, criarRequestValido(novaData), motoristaId);

            assertEquals(caronaId, response.id());
            assertEquals(novaData, carona.getDataHoraPartida());
        }

        @Test
        @DisplayName("Não deve atualizar se usuário não for o motorista")
        void naoDeveAtualizarSeNaoForMotorista() {
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));

            assertThrows(AcessoNegadoException.class, () ->
                    caronaService.atualizar(caronaId, criarRequestValido(LocalDateTime.now().plusDays(1)), outroUsuarioId));
        }

        @Test
        @DisplayName("Não deve atualizar quando não for informada exatamente uma data")
        void naoDeveAtualizarComMaisDeUmaData() {
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));

            assertThrows(RegraDeNegocioException.class, () ->
                    caronaService.atualizar(
                            caronaId,
                            criarRequestValido(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)),
                            motoristaId));
        }

        @Test
        @DisplayName("Não deve atualizar quando a lista de datas for nula")
        void naoDeveAtualizarQuandoListaDeDatasForNula() {
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));

            CaronaRequestDTO request = new CaronaRequestDTO(
                    veiculoId, new EnderecoDTO("Bodocongó", ORIGEM_LAT, ORIGEM_LON),
                    new EnderecoDTO("UFCG", DESTINO_LAT, DESTINO_LON), "Portaria",
                    null, 4, new BigDecimal("4.00"), null);

            assertThrows(RegraDeNegocioException.class, () -> caronaService.atualizar(caronaId, request, motoristaId));
        }

        @Test
        @DisplayName("Não deve atualizar carona que não está CRIADA")
        void naoDeveAtualizarComStatusInvalido() {
            carona.setStatus(StatusCarona.EM_ANDAMENTO);
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));

            assertThrows(RegraDeNegocioException.class, () ->
                    caronaService.atualizar(caronaId, criarRequestValido(LocalDateTime.now().plusDays(1)), motoristaId));
        }

        @Test
        @DisplayName("Não deve atualizar quando a nova data não for futura")
        void naoDeveAtualizarComDataNaoFutura() {
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));

            assertThrows(RegraDeNegocioException.class, () ->
                    caronaService.atualizar(caronaId, criarRequestValido(LocalDateTime.now().minusDays(1)), motoristaId));
        }

        @Test
        @DisplayName("Não deve atualizar quando quantidade de vagas for inválida")
        void naoDeveAtualizarComVagasInvalidas() {
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));

            CaronaRequestDTO request = new CaronaRequestDTO(
                    veiculoId, new EnderecoDTO("Bodocongó", ORIGEM_LAT, ORIGEM_LON),
                    new EnderecoDTO("UFCG", DESTINO_LAT, DESTINO_LON), "Portaria",
                    List.of(LocalDateTime.now().plusDays(1)), 0, new BigDecimal("4.00"), null);

            assertThrows(RegraDeNegocioException.class, () -> caronaService.atualizar(caronaId, request, motoristaId));
        }

        @Test
        @DisplayName("Não deve reduzir vagas abaixo da quantidade de passageiros já aceitos")
        void naoDeveReduzirVagasAbaixoDosAceitos() {
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));
            when(reservaCaronaRepository.somarPassageirosPorCaronaEStatus(caronaId, StatusReserva.ACEITA)).thenReturn(3);

            CaronaRequestDTO request = new CaronaRequestDTO(
                    veiculoId, new EnderecoDTO("Bodocongó", ORIGEM_LAT, ORIGEM_LON),
                    new EnderecoDTO("UFCG", DESTINO_LAT, DESTINO_LON), "Portaria",
                    List.of(LocalDateTime.now().plusDays(1)), 2, new BigDecimal("4.00"), null);

            assertThrows(RegraDeNegocioException.class, () -> caronaService.atualizar(caronaId, request, motoristaId));
        }

        @Test
        @DisplayName("Não deve atualizar quando valor de contribuição ultrapassa o limite")
        void naoDeveAtualizarQuandoValorUltrapassaLimite() {
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));
            when(reservaCaronaRepository.somarPassageirosPorCaronaEStatus(caronaId, StatusReserva.ACEITA)).thenReturn(0);

            CaronaRequestDTO request = new CaronaRequestDTO(
                    veiculoId, new EnderecoDTO("Bodocongó", ORIGEM_LAT, ORIGEM_LON),
                    new EnderecoDTO("UFCG", DESTINO_LAT, DESTINO_LON), "Portaria",
                    List.of(LocalDateTime.now().plusDays(1)), 4, new BigDecimal("100.00"), null);

            assertThrows(RegraDeNegocioException.class, () -> caronaService.atualizar(caronaId, request, motoristaId));
        }
    }

    @Nested
    @DisplayName("Cancelar carona")
    class CancelarCarona {

        @Test
        @DisplayName("Deve cancelar carona e as reservas ativas")
        void deveCancelarCarona() {
            ReservaCarona reserva = mock(ReservaCarona.class);
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));
            when(reservaCaronaRepository.findByCaronaIdAndStatusIn(eq(caronaId), anyList())).thenReturn(List.of(reserva));
            when(caronaRepository.save(any(Carona.class))).thenAnswer(inv -> inv.getArgument(0));

            CaronaResponseDTO response = caronaService.cancelar(caronaId, motoristaId);

            assertEquals(StatusCarona.CANCELADA, carona.getStatus());
            assertEquals(StatusCarona.CANCELADA, response.status());
            verify(reserva).setStatus(StatusReserva.CANCELADA);
            verify(reservaCaronaRepository).saveAll(List.of(reserva));
        }

        @Test
        @DisplayName("Não deve cancelar se usuário não for o motorista")
        void naoDeveCancelarSeNaoForMotorista() {
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));

            assertThrows(AcessoNegadoException.class, () -> caronaService.cancelar(caronaId, outroUsuarioId));
        }

        @ParameterizedTest
        @EnumSource(value = StatusCarona.class, names = {"FINALIZADA", "CANCELADA", "EM_ANDAMENTO"})
        @DisplayName("Não deve cancelar carona com status inválido")
        void naoDeveCancelarComStatusInvalido(StatusCarona status) {
            carona.setStatus(status);
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));

            assertThrows(EstadoInvalidoException.class, () -> caronaService.cancelar(caronaId, motoristaId));

            verify(caronaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Listar passageiros")
    class ListarPassageiros {
        @Test
        @DisplayName("Deve listar passageiros aceitos")
        void deveListarPassageiros() {
            when(caronaRepository.findById(caronaId)).thenReturn(Optional.of(carona));
            Usuario passageiro = new Usuario();
            passageiro.setId(2L);
            passageiro.setNome("João");
            ReservaCarona reserva = new ReservaCarona();
            reserva.setUsuario(passageiro);
            reserva.setCarona(carona);
            reserva.setStatus(StatusReserva.ACEITA);
            List<ReservaCarona> reservas = List.of(reserva);
            when(reservaCaronaRepository.findByCaronaIdAndStatus(caronaId, StatusReserva.ACEITA))
                    .thenReturn(reservas);
            List<PassageiroResponseDTO> resultado = caronaService.listarPassageiros(caronaId, motoristaId);
            assertNotNull(resultado);
            assertEquals(1, resultado.size());
            verify(reservaCaronaRepository).findByCaronaIdAndStatus(caronaId, StatusReserva.ACEITA);
        }
        @Test
        @DisplayName("Não deve listar passageiros se usuário não for motorista")
        void naoDeveListarPassageirosUsuarioNaoMotorista() {
            when(caronaRepository.findById(caronaId)).thenReturn(Optional.of(carona));
            assertThrows(AcessoNegadoException.class, () ->
                    caronaService.listarPassageiros(caronaId, outroUsuarioId)
            );
        }
    }
    @Nested
    @DisplayName("Iniciar carona")
    class IniciarCarona {
        @Test
        @DisplayName("Deve iniciar carona quando status estiver CRIADA")
        void deveIniciarCarona() {
            carona.setStatus(StatusCarona.CRIADA);
            carona.setDataHoraPartida(LocalDateTime.now());
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));
            caronaService.iniciarCarona(caronaId, motoristaId);
            assertEquals(StatusCarona.EM_ANDAMENTO, carona.getStatus());
            verify(caronaRepository).save(carona);
        }

        @Test
        @DisplayName("Não deve iniciar carona antes do dia agendado")
        void naoDeveIniciarAntesDoDiaAgendado() {
        carona.setStatus(StatusCarona.CRIADA);
        carona.setDataHoraPartida(LocalDateTime.now().plusDays(1));
        when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));

        assertThrows(RegraDeNegocioException.class, () ->
                caronaService.iniciarCarona(caronaId, motoristaId)
        );
        verify(caronaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Não deve iniciar carona após o dia agendado")
        void naoDeveIniciarAposODiaAgendado() {
        carona.setStatus(StatusCarona.CRIADA);
        carona.setDataHoraPartida(LocalDateTime.now().minusDays(1));
        when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));

        assertThrows(RegraDeNegocioException.class, () ->
                caronaService.iniciarCarona(caronaId, motoristaId)
        );
        verify(caronaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Não deve iniciar carona com status inválido")
        void naoDeveIniciarComStatusInvalido() {
            carona.setStatus(StatusCarona.FINALIZADA);
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));
            assertThrows(EstadoInvalidoException.class, () ->
                    caronaService.iniciarCarona(caronaId, motoristaId)
            );
            verify(caronaRepository, never()).save(any());
        }
    }
    @Nested
    @DisplayName("Finalizar carona")
    class FinalizarCarona {
        @Test
        @DisplayName("Deve finalizar carona e concluir reservas")
        void deveFinalizarCarona() {
            carona.setStatus(StatusCarona.EM_ANDAMENTO);
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));
            ReservaCarona reserva = mock(ReservaCarona.class);
            List<ReservaCarona> reservas = List.of(reserva);
            when(reservaCaronaRepository.findByCaronaIdAndStatus(caronaId, StatusReserva.ACEITA))
                    .thenReturn(reservas);
            caronaService.finalizarCarona(caronaId, motoristaId);
            assertEquals(StatusCarona.FINALIZADA, carona.getStatus());
            verify(reserva).setStatus(StatusReserva.CONCLUIDA);
            verify(reservaCaronaRepository).saveAll(reservas);
            verify(caronaRepository).save(carona);
        }
        @Test
        @DisplayName("Não deve finalizar carona com status inválido")
        void naoDeveFinalizarComStatusInvalido() {
            carona.setStatus(StatusCarona.CANCELADA);
            when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.of(carona));
            assertThrows(EstadoInvalidoException.class, () ->
                    caronaService.finalizarCarona(caronaId, motoristaId)
            );
            verify(caronaRepository, never()).save(any());
        }
    }
    @Test
    @DisplayName("Deve lançar exceção quando carona não existir")
    void deveLancarErroQuandoCaronaNaoExiste() {
        when(caronaRepository.findByIdForUpdate(caronaId)).thenReturn(Optional.empty());
        assertThrows(CaronaNaoEncontradaException.class, () ->
                caronaService.iniciarCarona(caronaId, motoristaId)
        );
    }
}
