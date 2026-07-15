package com.unicar.dto.carona;

import com.unicar.domain.Usuario;

public record UsuarioResumoDTO(
    Long id,
    String nome
) {
    public UsuarioResumoDTO(Usuario usuario) {
        this(usuario.getId(), usuario.getNome());
    }
}
