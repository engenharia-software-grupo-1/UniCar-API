package com.unicar.dto.carona;

import com.unicar.domain.ReservaCarona;

public record PassageiroResponseDTO(
        Long reservaId,
        Long usuarioId,
        String nome,
        Integer quantidadePassageiros
) {

    public static PassageiroResponseDTO from(ReservaCarona reserva) {
        return new PassageiroResponseDTO(
                reserva.getId(),
                reserva.getUsuario().getId(),
                reserva.getUsuario().getNome(),
                reserva.getQuantidadePassageiros()
        );
    }
}