package com.unicar.dto.carona;

import com.unicar.domain.ReservaCarona;

public record PassageiroResponseDTO(
        Long reservaId,
        Long usuarioId,
        String nome,
        String linkFoto,
        Integer quantidadePassageiros,
        EnderecoDTO embarque
) {

    public PassageiroResponseDTO(ReservaCarona reserva) {
        this(
                reserva.getId(),
                reserva.getUsuario().getId(),
                reserva.getUsuario().getNome(),
                reserva.getUsuario().getLinkFoto(),
                reserva.getQuantidadePassageiros(),
                new EnderecoDTO(reserva.getOrigemEmbarqueDescricao(), reserva.getOrigemEmbarqueLatitude(), reserva.getOrigemEmbarqueLongitude())
        );
    }
}
