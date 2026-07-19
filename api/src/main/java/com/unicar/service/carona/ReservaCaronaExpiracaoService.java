package com.unicar.service.carona;

import com.unicar.domain.ReservaCarona;
import com.unicar.enums.StatusReserva;
import com.unicar.enums.TipoNotificacao;
import com.unicar.repository.ReservaCaronaRepository;
import com.unicar.service.NotificacaoService;
import com.unicar.util.notificacoes.NotificacaoTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservaCaronaExpiracaoService {

    private final ReservaCaronaRepository reservaCaronaRepository;
    private final NotificacaoService notificacaoService;

    @Scheduled(fixedDelayString = "${unicar.reserva.expiracao.intervalo-ms:60000}")
    @Transactional
    public void removerReservasPendentesExpiradas() {
        List<ReservaCarona> reservasExpiradas = reservaCaronaRepository
                .buscarReservasPendentesExpiradasParaAtualizacao(LocalDateTime.now());

        if (reservasExpiradas.isEmpty()) {
            return;
        }

        reservasExpiradas.forEach(reserva -> reserva.setStatus(StatusReserva.REMOVIDA));
        reservaCaronaRepository.saveAll(reservasExpiradas);

        reservasExpiradas.forEach(reserva -> notificacaoService.dispararNotificacaoSistemica(
                reserva.getUsuario(),
                "Reserva expirada",
                NotificacaoTemplates.reservaExpirada(reserva.getCarona()),
                TipoNotificacao.RESERVA_EXPIRADA
        ));

        log.info("Reservas expiradas marcadas como REMOVIDA: {}", reservasExpiradas.size());
    }
}
