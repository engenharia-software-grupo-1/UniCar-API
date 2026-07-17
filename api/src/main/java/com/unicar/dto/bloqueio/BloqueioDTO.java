package com.unicar.dto.bloqueio;

import com.unicar.domain.BloqueioUsuario;

import java.time.LocalDateTime;

public record BloqueioDTO(
        Long id,
        Long usuarioId,
        Long usuarioBloqueadoId,
        LocalDateTime dataBloqueio
) {

    public static BloqueioDTO from(BloqueioUsuario bloqueio) {
        return new BloqueioDTO(
                bloqueio.getId(),
                bloqueio.getUsuario().getId(),
                bloqueio.getUsuarioBloqueado().getId(),
                bloqueio.getDataBloqueio()
        );
    }

}
