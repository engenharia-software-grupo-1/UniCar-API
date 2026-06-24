package com.unicar.dto.auth;

import com.unicar.dto.usuario.UsuarioDTO;

public record LoginResponseDTO(
    String token,
    UsuarioDTO usuario
) {
}
