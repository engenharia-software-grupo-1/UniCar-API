package com.unicar.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
    @NotBlank(message = "O campo 'usuario' é obrigatório.")
    String usuario,
    @NotBlank(message = "O campo 'senha' é obrigatório.")
    String senha
) {
}
