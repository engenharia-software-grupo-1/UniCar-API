package com.unicar.service.carona;
import com.unicar.domain.Carona;
import com.unicar.domain.ReservaCarona;
import com.unicar.domain.Usuario;
import com.unicar.domain.Veiculo;
import com.unicar.dto.avaliacao.ReputacaoDTO;
import com.unicar.dto.carona.BuscaCaronaFiltroDTO;
import com.unicar.dto.carona.CaronaBuscaResponseDTO;
import com.unicar.dto.carona.CaronaRequestDTO;
import com.unicar.dto.carona.EnderecoDTO;
import com.unicar.dto.carona.PassageiroResponseDTO;
import com.unicar.enums.StatusCarona;
import com.unicar.enums.StatusReserva;
import com.unicar.exception.AcessoNegadoException;
import com.unicar.exception.CaronaNaoEncontradaException;
import com.unicar.exception.EstadoInvalidoException;
import com.unicar.exception.RegraDeNegocioException;
import com.unicar.repository.CaronaRepository;
import com.unicar.repository.ReservaCaronaRepository;
import com.unicar.repository.UsuarioRepository;
import com.unicar.repository.VeiculoRepository;

import com.unicar.service.AvaliacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
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
    private ReservaCaronaRepository reservaCaronaRepository;
    @Mock
    private AvaliacaoService avaliacaoService;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private VeiculoRepository veiculoRepository;
    @InjectMocks
    private CaronaService caronaService;
    private final Long caronaId = 1L;
    private final Long motoristaId = 10L;
    private final Long outroUsuarioId = 20L;

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
        veiculo.setId(1L);
        veiculo.setUsuario(motorista);

        carona = new Carona();
        carona.setId(caronaId);
        carona.setMotorista(motorista);
        carona.setDataHoraPartida(LocalDateTime.now());
    }

    private CaronaRequestDTO criarRequestValido(Integer quantidadeVagas, BigDecimal valorContribuicao) {
        return new CaronaRequestDTO(
                veiculo.getId(),
                new EnderecoDTO("Bodocongó", ORIGEM_LAT, ORIGEM_LON),
                new EnderecoDTO("UFCG", DESTINO_LAT, DESTINO_LON),
                "Portaria principal",
                List.of(LocalDateTime.now().plusDays(1)),
                quantidadeVagas,
                valorContribuicao,
                null
        );
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

    @ParameterizedTest(name = "vagas={0} -> aceita={1}")
    @CsvSource({
            "-1,false",
            "0,false",
            "1,true",
            "100,true"
    })
    @DisplayName("Deve validar limite mínimo da quantidade de vagas ao criar carona")
    void vagasLimiteDeCriacao(int quantidadeVagas, boolean deveSerAceita) {
        CaronaRequestDTO request = criarRequestValido(quantidadeVagas, new BigDecimal("1.00"));

        when(usuarioRepository.findByIdForUpdate(motoristaId)).thenReturn(Optional.of(motorista));
        when(veiculoRepository.findById(veiculo.getId())).thenReturn(Optional.of(veiculo));

        if (deveSerAceita) {
            when(caronaRepository.existsByMotorista_IdAndStatus(motoristaId, StatusCarona.EM_ANDAMENTO)).thenReturn(false);
            when(caronaRepository.existsByMotoristaIdAndDataHoraPartidaAndStatusIn(eq(motoristaId), any(), anyList())).thenReturn(false);
            when(caronaRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> caronaService.criar(request, motoristaId));
        } else {
            assertThrows(RegraDeNegocioException.class, () -> caronaService.criar(request, motoristaId));
            verify(caronaRepository, never()).saveAll(any());
        }
    }

    @ParameterizedTest(name = "valor={0} -> aceita={1}")
    @CsvSource({
            "0.00,true",
            "4.54,true",
            "4.55,true",
            "4.56,false"
    })
    @DisplayName("Deve validar limite do valor de contribuição pela distância do trajeto")
    void valorLimiteDeCriacao(BigDecimal valorContribuicao, boolean deveSerAceita) {
        CaronaRequestDTO request = criarRequestValido(4, valorContribuicao);

        when(usuarioRepository.findByIdForUpdate(motoristaId)).thenReturn(Optional.of(motorista));
        when(veiculoRepository.findById(veiculo.getId())).thenReturn(Optional.of(veiculo));
        when(caronaRepository.existsByMotorista_IdAndStatus(motoristaId, StatusCarona.EM_ANDAMENTO)).thenReturn(false);

        if (deveSerAceita) {
            when(caronaRepository.existsByMotoristaIdAndDataHoraPartidaAndStatusIn(eq(motoristaId), any(), anyList())).thenReturn(false);
            when(caronaRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> caronaService.criar(request, motoristaId));
        } else {
            assertThrows(RegraDeNegocioException.class, () -> caronaService.criar(request, motoristaId));
            verify(caronaRepository, never()).saveAll(any());
        }
    }
}