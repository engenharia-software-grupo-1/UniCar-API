package com.unicar.service.avaliacoes;

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
import com.unicar.exception.RegraDeNegocioException;
import com.unicar.repository.AvaliacaoRepository;
import com.unicar.repository.CaronaRepository;
import com.unicar.repository.ReservaCaronaRepository;
import com.unicar.repository.UsuarioRepository;
import com.unicar.service.AvaliacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvaliacaoServiceTest {

    @Mock
    private AvaliacaoRepository avaliacaoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CaronaRepository caronaRepository;

    @Mock
    private ReservaCaronaRepository reservaCaronaRepository;

    @InjectMocks
    private AvaliacaoService avaliacaoService;

    private Usuario motorista;
    private Usuario passageiro;
    private Carona carona;

    @BeforeEach
    void setUp() {
        motorista = new Usuario();
        motorista.setId(1L);
        motorista.setNome("Motorista");
        motorista.setLinkFoto("https://cdn.exemplo.com/motorista.jpg");

        passageiro = new Usuario();
        passageiro.setId(2L);
        passageiro.setNome("Passageiro");

        carona = new Carona();
        carona.setId(1L);
        carona.setMotorista(motorista);
        carona.setStatus(StatusCarona.FINALIZADA);
    }

    @Nested
    @DisplayName("Avaliar")
    class Avaliar {

        @Test
        @DisplayName("Deve avaliar com sucesso e retornar o ID da avaliação")
        void deveAvaliar() {
            AvaliacaoRequestDTO dto = new AvaliacaoRequestDTO(1L, 2L, 5, "Ótima viagem");
            Avaliacao avaliacaoSalva = Avaliacao.builder().id(100L).build();

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(motorista));
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(passageiro));
            when(caronaRepository.findById(1L)).thenReturn(Optional.of(carona));

            when(reservaCaronaRepository.existsByCaronaIdAndUsuarioId(1L, 1L)).thenReturn(false);
            when(reservaCaronaRepository.existsByCaronaIdAndUsuarioId(1L, 2L)).thenReturn(true);
            when(avaliacaoRepository.existsByCaronaIdAndAvaliadorIdAndAvaliadoId(1L, 1L, 2L)).thenReturn(false);

            when(avaliacaoRepository.save(any(Avaliacao.class))).thenReturn(avaliacaoSalva);

            Long idCriado = avaliacaoService.avaliar(1L, dto);

            assertEquals(100L, idCriado);
            verify(avaliacaoRepository).save(any(Avaliacao.class));
        }

        @Test
        @DisplayName("Não deve permitir nota inválida")
        void naoDevePermitirNotaInvalida() {
            AvaliacaoRequestDTO dto = new AvaliacaoRequestDTO(1L, 2L, 6, "");

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(motorista));
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(passageiro));
            when(caronaRepository.findById(1L)).thenReturn(Optional.of(carona));

            assertThrows(RegraDeNegocioException.class, () -> avaliacaoService.avaliar(1L, dto));
            verify(avaliacaoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Não deve avaliar carona não finalizada")
        void naoDeveAvaliarCaronaNaoFinalizada() {
            carona.setStatus(StatusCarona.CRIADA);
            AvaliacaoRequestDTO dto = new AvaliacaoRequestDTO(1L, 2L, 5, "");

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(motorista));
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(passageiro));
            when(caronaRepository.findById(1L)).thenReturn(Optional.of(carona));

            assertThrows(RegraDeNegocioException.class, () -> avaliacaoService.avaliar(1L, dto));
            verify(avaliacaoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Não deve permitir avaliação duplicada")
        void naoDeveAvaliarDuasVezes() {
            AvaliacaoRequestDTO dto = new AvaliacaoRequestDTO(1L, 2L, 5, "");

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(motorista));
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(passageiro));
            when(caronaRepository.findById(1L)).thenReturn(Optional.of(carona));

            when(reservaCaronaRepository.existsByCaronaIdAndUsuarioId(1L, 1L)).thenReturn(false);
            when(reservaCaronaRepository.existsByCaronaIdAndUsuarioId(1L, 2L)).thenReturn(true);
            when(avaliacaoRepository.existsByCaronaIdAndAvaliadorIdAndAvaliadoId(1L, 1L, 2L)).thenReturn(true);

            assertThrows(RegraDeNegocioException.class, () -> avaliacaoService.avaliar(1L, dto));
            verify(avaliacaoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve listar avaliações recebidas chamando o método ordenado")
        void deveListarAvaliacoes() {
            Avaliacao avaliacao = new Avaliacao();
            avaliacao.setId(1L);
            avaliacao.setNota(5);
            avaliacao.setComentario("Muito bom");
            avaliacao.setAvaliador(motorista);
            avaliacao.setAvaliado(passageiro);
            avaliacao.setCarona(carona);

            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(passageiro));

            when(avaliacaoRepository.findByAvaliadoIdOrderByDataAvaliacaoDesc(2L)).thenReturn(List.of(avaliacao));

            List<AvaliacaoRecebidaDTO> resultado = avaliacaoService.listarAvaliacoesRecebidas(2L);

            assertEquals(1, resultado.size());
            assertEquals(5, resultado.getFirst().nota());
            assertEquals("https://cdn.exemplo.com/motorista.jpg", resultado.getFirst().avaliador().linkFoto());
        }

        @Test
        @DisplayName("Deve buscar reputação com arredondamento correto")
        void deveBuscarReputacao() {
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(passageiro));
            when(avaliacaoRepository.calcularMedia(2L)).thenReturn(4.5333);
            when(avaliacaoRepository.countByAvaliadoId(2L)).thenReturn(2L);

            ReputacaoDTO reputacao = avaliacaoService.buscarReputacao(2L);

            assertEquals(4.5, reputacao.media());
            assertEquals(2L, reputacao.quantidadeAvaliacoes());
        }
    }

    @Nested
    @DisplayName("ListarAvaliacoesPendentes")
    class ListarAvaliacoesPendentes {

        @Test
        @DisplayName("Deve retornar motorista pendente quando o passageiro consultar")
        void deveRetornarMotoristaPendente() {
            when(caronaRepository.findById(1L)).thenReturn(Optional.of(carona));
            when(reservaCaronaRepository.existsByCaronaIdAndUsuarioId(1L, 2L)).thenReturn(true);
            when(avaliacaoRepository.existsByCaronaIdAndAvaliadorIdAndAvaliadoId(1L, 2L, 1L)).thenReturn(false);

            List<ParticipantePendenteDTO> pendentes = avaliacaoService.listarAvaliacoesPendentes(1L, 2L);

            assertEquals(1, pendentes.size());
            assertEquals(1L, pendentes.getFirst().usuarioId());
            assertEquals("MOTORISTA", pendentes.getFirst().tipo());
        }

        @Test
        @DisplayName("Deve retornar passageiros pendentes aceitos quando o motorista consultar")
        void deveRetornarPassageirosPendentes() {
            ReservaCarona reserva = new ReservaCarona();
            reserva.setUsuario(passageiro);
            reserva.setStatus(StatusReserva.ACEITA);

            when(caronaRepository.findById(1L)).thenReturn(Optional.of(carona));
            when(reservaCaronaRepository.existsByCaronaIdAndUsuarioId(1L, 1L)).thenReturn(false);
            when(reservaCaronaRepository.findByCaronaIdAndStatus(1L, StatusReserva.ACEITA)).thenReturn(List.of(reserva));
            when(avaliacaoRepository.existsByCaronaIdAndAvaliadorIdAndAvaliadoId(1L, 1L, 2L)).thenReturn(false);

            List<ParticipantePendenteDTO> pendentes = avaliacaoService.listarAvaliacoesPendentes(1L, 1L);

            assertEquals(1, pendentes.size());
            assertEquals(2L, pendentes.getFirst().usuarioId());
            assertEquals("PASSAGEIRO", pendentes.getFirst().tipo());
        }

        @Test
        @DisplayName("Deve lançar exceção se um usuário não participante tentar ver as pendências")
        void deveLancarExcecaoParaNaoParticipante() {
            when(caronaRepository.findById(1L)).thenReturn(Optional.of(carona));
            when(reservaCaronaRepository.existsByCaronaIdAndUsuarioId(1L, 99L)).thenReturn(false);

            assertThrows(RegraDeNegocioException.class,
                    () -> avaliacaoService.listarAvaliacoesPendentes(1L, 99L));
        }
    }
}
