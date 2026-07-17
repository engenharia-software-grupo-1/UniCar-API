package com.unicar.dto.carona;

import com.unicar.domain.ReservaCarona;
import com.unicar.enums.StatusReserva;

import java.math.BigDecimal;

public record ReservaResponseDTO(
    Long id,
    StatusReserva status,
    Integer quantidadePassageiros,
    BigDecimal valorContribuicao
) {
    public ReservaResponseDTO(ReservaCarona reserva) {
        this(
            reserva.getId(),
            reserva.getStatus(),
            reserva.getQuantidadePassageiros(),
            reserva.getValorContribuicao()
        );
    }
}
