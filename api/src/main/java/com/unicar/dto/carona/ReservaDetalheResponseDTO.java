package com.unicar.dto.carona;

import com.unicar.domain.ReservaCarona;
import com.unicar.enums.StatusReserva;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservaDetalheResponseDTO(
    Long id,
    StatusReserva status,
    Integer quantidadePassageiros,
    BigDecimal valorContribuicao,
    EnderecoDTO origemEmbarque,
    CaronaResumoDTO carona,
    LocalDateTime dataSolicitacao
) {
    public ReservaDetalheResponseDTO(ReservaCarona reserva) {
        this(
            reserva.getId(),
            reserva.getStatus(),
            reserva.getQuantidadePassageiros(),
            reserva.getValorContribuicao(),
            new EnderecoDTO(
                reserva.getOrigemEmbarqueDescricao(),
                reserva.getOrigemEmbarqueLatitude(),
                reserva.getOrigemEmbarqueLongitude()
            ),
            new CaronaResumoDTO(reserva.getCarona()),
            reserva.getDataReserva()
        );
    }
}
