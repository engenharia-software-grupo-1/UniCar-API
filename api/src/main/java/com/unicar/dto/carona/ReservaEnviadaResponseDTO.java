package com.unicar.dto.carona;

import com.unicar.domain.ReservaCarona;
import com.unicar.enums.StatusReserva;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservaEnviadaResponseDTO(
    Long id,
    CaronaResumoDTO carona,
    StatusReserva status,
    Integer quantidadePassageiros,
    BigDecimal valorContribuicao,
    LocalDateTime dataCarona
) {
    public ReservaEnviadaResponseDTO(ReservaCarona reserva) {
        this(
            reserva.getId(),
            new CaronaResumoDTO(reserva.getCarona()),
            reserva.getStatus(),
            reserva.getQuantidadePassageiros(),
            reserva.getValorContribuicao(),
            reserva.getCarona().getDataHoraPartida()
        );
    }
}
