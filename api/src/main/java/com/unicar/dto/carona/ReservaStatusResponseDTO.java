package com.unicar.dto.carona;

import com.unicar.domain.ReservaCarona;
import com.unicar.enums.StatusReserva;

public record ReservaStatusResponseDTO(
    Long id,
    StatusReserva status
) {
    public ReservaStatusResponseDTO(ReservaCarona reserva) {
        this(reserva.getId(), reserva.getStatus());
    }
}
