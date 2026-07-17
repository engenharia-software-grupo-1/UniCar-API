package com.unicar.dto.veiculo;

import com.unicar.enums.TipoVeiculo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VeiculoRequestDTO(
    @NotBlank(message = "Modelo é obrigatório") String modelo,
    @NotBlank(message = "Placa é obrigatória") String placa,
    String cor,
    @NotNull(message = "Tipo do veículo é obrigatório") TipoVeiculo tipoVeiculo
) {
}
