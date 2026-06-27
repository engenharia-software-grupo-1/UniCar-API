package com.unicar.dto.bloqueio;

import com.unicar.domain.BloqueioUsuario;
import com.unicar.domain.Usuario;

import java.time.LocalDateTime;

public record UsuarioBloqueadoDTO(
        Long id,
        String nome,
        String curso,
        LocalDateTime dataBloqueio
) {

    public static UsuarioBloqueadoDTO from(BloqueioUsuario bloqueio) {
        Usuario usuario = bloqueio.getUsuarioBloqueado();

        return new UsuarioBloqueadoDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getCurso(),
                bloqueio.getDataBloqueio()
        );
    }
}