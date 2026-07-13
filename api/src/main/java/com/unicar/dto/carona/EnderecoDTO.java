package com.unicar.dto.carona;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record EnderecoDTO(
    @NotBlank(message = "Descrição do endereço é obrigatória")
    String descricao,

    @NotNull(message = "Latitude é obrigatória")
    BigDecimal latitude,

    @NotNull(message = "Longitude é obrigatória")
    BigDecimal longitude
) {}