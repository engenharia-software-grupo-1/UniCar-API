package com.unicar.dto.carona;

import com.unicar.domain.Usuario;

public record UsuarioResumoDTO(
    Long id,
    String nome,
    String linkFoto
) {
    public UsuarioResumoDTO(Usuario usuario) {
        this(usuario.getId(), usuario.getNome(), usuario.getLinkFoto());
    }
}
