package com.unicar.dto.usuario;

import com.unicar.domain.Usuario;

public record UsuarioPublicoDTO(
        Long id,
        String matricula,
        String nome,
        String email,
        String curso
) {
    public static UsuarioPublicoDTO from(Usuario usuario) {
        return new UsuarioPublicoDTO(
                usuario.getId(),
                usuario.getMatricula(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getCurso()
        );
    }
}