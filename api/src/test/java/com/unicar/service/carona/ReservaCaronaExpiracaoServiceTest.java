package com.unicar.service.carona;

import com.unicar.domain.Carona;
import com.unicar.domain.ReservaCarona;
import com.unicar.domain.Usuario;
import com.unicar.enums.StatusReserva;
import com.unicar.enums.TipoNotificacao;
import com.unicar.repository.ReservaCaronaRepository;
import com.unicar.service.NotificacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaCaronaExpiracaoServiceTest {

    @Mock
    private ReservaCaronaRepository reservaCaronaRepository;

    @Mock
    private NotificacaoService notificacaoService;

    @InjectMocks
    private ReservaCaronaExpiracaoService service;

    private ReservaCarona reserva;

    @BeforeEach
    void setUp() {
        Usuario passageiro = Usuario.builder().id(10L).build();
        Carona carona = Carona.builder()
                .id(20L)
                .destinoDescricao("UFCG")
                .build();
        reserva = ReservaCarona.builder()
                .id(30L)
                .usuario(passageiro)
                .carona(carona)
                .status(StatusReserva.PENDENTE)
                .dataExpiracao(LocalDateTime.now().minusMinutes(1))
                .build();
    }

    @Test
    void deveRemoverEEnviarNotificacaoParaReservasComPrazoAtingido() {
        LocalDateTime inicio = LocalDateTime.now();
        when(reservaCaronaRepository.buscarReservasPendentesExpiradasParaAtualizacao(any()))
                .thenReturn(List.of(reserva));

        service.removerReservasPendentesExpiradas();

        ArgumentCaptor<LocalDateTime> agora = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(reservaCaronaRepository).buscarReservasPendentesExpiradasParaAtualizacao(agora.capture());
        assertFalse(agora.getValue().isBefore(inicio));
        assertEquals(StatusReserva.REMOVIDA, reserva.getStatus());
        verify(reservaCaronaRepository).saveAll(List.of(reserva));
        verify(notificacaoService).dispararNotificacaoSistemica(
                eq(reserva.getUsuario()),
                eq("Reserva expirada"),
                contains("UFCG"),
                eq(TipoNotificacao.RESERVA_EXPIRADA)
        );
    }

    @Test
    void naoDeveSalvarOuNotificarQuandoNaoHouverReservaExpirada() {
        when(reservaCaronaRepository.buscarReservasPendentesExpiradasParaAtualizacao(any()))
                .thenReturn(List.of());

        service.removerReservasPendentesExpiradas();

        verify(reservaCaronaRepository, never()).saveAll(anyList());
        verifyNoInteractions(notificacaoService);
    }
}
