package com.unicar.service.historico;

import com.unicar.domain.Carona;
import com.unicar.domain.ReservaCarona;
import com.unicar.domain.Usuario;
import com.unicar.dto.historico.DetalhesHistoricoResponseDTO;
import com.unicar.dto.historico.HistoricoMotoristaResponseDTO;
import com.unicar.dto.historico.HistoricoPassageiroResponseDTO;
import com.unicar.enums.StatusCarona;
import com.unicar.enums.StatusReserva;
import com.unicar.repository.CaronaRepository;
import com.unicar.repository.ReservaCaronaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricoServiceTest {

    @Mock
    private CaronaRepository caronaRepository;

    @Mock
    private ReservaCaronaRepository reservaCaronaRepository;

    @InjectMocks
    private HistoricoService historicoService;

    private final Long motoristaId = 10L;
    private final Long passageiroId = 20L;
    private final Long caronaId = 100L;
    private final Long outroUsuarioId = 99L;

    private Usuario motorista;
    private Usuario passageiro;
    private Carona carona;

    @BeforeEach
    void setUp() {
        motorista = new Usuario();
        motorista.setId(motoristaId);
        motorista.setNome("João Motorista");

        passageiro = new Usuario();
        passageiro.setId(passageiroId);
        passageiro.setNome("Maria Passageira");

        carona = new Carona();
        carona.setId(caronaId);
        carona.setMotorista(motorista);
        carona.setStatus(StatusCarona.FINALIZADA);
        carona.setOrigemDescricao("Bodocongó");
        carona.setDestinoDescricao("UFCG");
        carona.setDataHoraPartida(LocalDateTime.now().minusDays(1));
    }

    @Nested
    @DisplayName("Listar histórico como motorista")
    class ListarHistoricoComoMotorista {

        @Test
        @DisplayName("Deve mapear a página de caronas com o total de passageiros de cada uma")
        void deveMapearHistoricoDoMotorista() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Carona> pagina = new PageImpl<>(List.of(carona), pageable, 1);

            when(caronaRepository.findHistoricoComoMotorista(motoristaId, pageable)).thenReturn(pagina);
            when(reservaCaronaRepository.somarPassageirosPorCaronaEStatusIn(eq(caronaId), anyList())).thenReturn(3);

            Page<HistoricoMotoristaResponseDTO> resultado =
                    historicoService.listarHistoricoComoMotorista(motoristaId, pageable);

            assertThat(resultado.getTotalElements()).isEqualTo(1);
            HistoricoMotoristaResponseDTO dto = resultado.getContent().getFirst();
            assertThat(dto.caronaId()).isEqualTo(caronaId);
            assertThat(dto.origem()).isEqualTo("Bodocongó");
            assertThat(dto.destino()).isEqualTo("UFCG");
            assertThat(dto.status()).isEqualTo(StatusCarona.FINALIZADA);
            assertThat(dto.totalPassageiros()).isEqualTo(3);
        }

        @Test
        @DisplayName("Deve retornar página vazia quando motorista não tem histórico")
        void deveRetornarPaginaVaziaSemHistorico() {
            Pageable pageable = PageRequest.of(0, 10);
            when(caronaRepository.findHistoricoComoMotorista(motoristaId, pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            Page<HistoricoMotoristaResponseDTO> resultado =
                    historicoService.listarHistoricoComoMotorista(motoristaId, pageable);

            assertThat(resultado.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Listar histórico como passageiro")
    class ListarHistoricoComoPassageiro {

        @Test
        @DisplayName("Deve mapear a página de reservas incluindo o nome do motorista")
        void deveMapearHistoricoDoPassageiro() {
            ReservaCarona reserva = new ReservaCarona();
            reserva.setId(1L);
            reserva.setUsuario(passageiro);
            reserva.setCarona(carona);

            Pageable pageable = PageRequest.of(0, 10);
            Page<ReservaCarona> pagina = new PageImpl<>(List.of(reserva), pageable, 1);

            when(reservaCaronaRepository.findHistoricoComoPassageiro(passageiroId, pageable)).thenReturn(pagina);
            when(reservaCaronaRepository.somarPassageirosPorCaronaEStatusIn(eq(caronaId), anyList())).thenReturn(2);

            Page<HistoricoPassageiroResponseDTO> resultado =
                    historicoService.listarHistoricoComoPassageiro(passageiroId, pageable);

            HistoricoPassageiroResponseDTO dto = resultado.getContent().getFirst();
            assertThat(dto.reservaId()).isEqualTo(1L);
            assertThat(dto.caronaId()).isEqualTo(caronaId);
            assertThat(dto.motorista()).isEqualTo("João Motorista");
            assertThat(dto.status()).isEqualTo(StatusCarona.FINALIZADA);
            assertThat(dto.quantidadePassageiros()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Obter detalhes da viagem")
    class ObterDetalhesViagem {

        @Test
        @DisplayName("Deve lançar 404 quando a carona não existir")
        void deveLancarNotFoundQuandoCaronaNaoExistir() {
            when(caronaRepository.findById(caronaId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> historicoService.obterDetalhesViagem(caronaId, motoristaId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Deve lançar 403 quando o usuário não participou da viagem")
        void deveLancarForbiddenQuandoUsuarioNaoParticipou() {
            when(caronaRepository.findById(caronaId)).thenReturn(Optional.of(carona));
            when(reservaCaronaRepository.existsByCaronaIdAndUsuarioId(caronaId, outroUsuarioId)).thenReturn(false);

            assertThatThrownBy(() -> historicoService.obterDetalhesViagem(caronaId, outroUsuarioId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Deve lançar 400 quando a carona ainda não estiver finalizada")
        void deveLancarBadRequestQuandoCaronaNaoFinalizada() {
            carona.setStatus(StatusCarona.EM_ANDAMENTO);
            when(caronaRepository.findById(caronaId)).thenReturn(Optional.of(carona));

            assertThatThrownBy(() -> historicoService.obterDetalhesViagem(caronaId, motoristaId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Deve retornar os detalhes quando o solicitante for o motorista")
        void deveRetornarDetalhesParaOMotorista() {
            ReservaCarona reservaAceita = new ReservaCarona();
            reservaAceita.setUsuario(passageiro);
            reservaAceita.setCarona(carona);
            reservaAceita.setStatus(StatusReserva.ACEITA);

            when(caronaRepository.findById(caronaId)).thenReturn(Optional.of(carona));
            when(reservaCaronaRepository.findByCaronaIdAndStatusIn(eq(caronaId), anyList()))
                    .thenReturn(List.of(reservaAceita));

            DetalhesHistoricoResponseDTO resultado = historicoService.obterDetalhesViagem(caronaId, motoristaId);

            assertThat(resultado.caronaId()).isEqualTo(caronaId);
            assertThat(resultado.motorista().id()).isEqualTo(motoristaId);
            assertThat(resultado.motorista().nome()).isEqualTo("João Motorista");
            assertThat(resultado.passageiros()).hasSize(1);
            assertThat(resultado.passageiros().getFirst().id()).isEqualTo(passageiroId);
        }

        @Test
        @DisplayName("Deve retornar os detalhes quando o solicitante for um passageiro")
        void deveRetornarDetalhesParaOPassageiro() {
            when(caronaRepository.findById(caronaId)).thenReturn(Optional.of(carona));
            when(reservaCaronaRepository.existsByCaronaIdAndUsuarioId(caronaId, passageiroId)).thenReturn(true);
            when(reservaCaronaRepository.findByCaronaIdAndStatusIn(eq(caronaId), anyList()))
                    .thenReturn(List.of());

            DetalhesHistoricoResponseDTO resultado = historicoService.obterDetalhesViagem(caronaId, passageiroId);

            assertThat(resultado.caronaId()).isEqualTo(caronaId);
            assertThat(resultado.passageiros()).isEmpty();
        }
    }
}
