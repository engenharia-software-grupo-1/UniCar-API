package com.unicar.dto.veiculo;

import jakarta.validation.constraints.NotBlank;

public record VeiculoRequestDTO(
    @NotBlank(message = "Modelo é obrigatório") String modelo,
    @NotBlank(message = "Placa é obrigatória") String placa,
    String cor
) {
}
