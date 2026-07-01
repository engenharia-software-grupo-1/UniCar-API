package com.unicar.dto;

import jakarta.validation.constraints.NotBlank;

public record VeiculoRequest(
        @NotBlank(message = "Modelo é obrigatório")
        String modelo,

        @NotBlank(message = "Placa é obrigatória")
        String placa,

        String cor
) {
}
