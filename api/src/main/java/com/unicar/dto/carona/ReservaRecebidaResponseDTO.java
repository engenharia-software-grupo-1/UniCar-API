package com.unicar.dto.carona;

import com.unicar.domain.ReservaCarona;
import com.unicar.enums.StatusReserva;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservaRecebidaResponseDTO(
    Long id,
    UsuarioResumoDTO usuario,
    EnderecoDTO origemEmbarque,
    Integer quantidadePassageiros,
    BigDecimal valorContribuicao,
    StatusReserva status,
    LocalDateTime dataSolicitacao
) {
    public ReservaRecebidaResponseDTO(ReservaCarona reserva) {
        this(
            reserva.getId(),
            new UsuarioResumoDTO(reserva.getUsuario()),
            new EnderecoDTO(
                reserva.getOrigemEmbarqueDescricao(),
                reserva.getOrigemEmbarqueLatitude(),
                reserva.getOrigemEmbarqueLongitude()
            ),
            reserva.getQuantidadePassageiros(),
            reserva.getValorContribuicao(),
            reserva.getStatus(),
            reserva.getDataReserva()
        );
    }
}
