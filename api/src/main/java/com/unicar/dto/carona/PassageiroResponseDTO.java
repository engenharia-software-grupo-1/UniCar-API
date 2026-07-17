package com.unicar.dto.carona;

import com.unicar.domain.ReservaCarona;

public record PassageiroResponseDTO(
        Long reservaId,
        Long usuarioId,
        String nome,
        Integer quantidadePassageiros
) {

    public PassageiroResponseDTO(ReservaCarona reserva) {
        this(
                reserva.getId(),
                reserva.getUsuario().getId(),
                reserva.getUsuario().getNome(),
                reserva.getQuantidadePassageiros()
        );
    }
}