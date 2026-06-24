package com.unicar.dto;

public record LoginResponseDTO(
        String token,
        UsuarioLogadoResponseDTO usuario
) {}
