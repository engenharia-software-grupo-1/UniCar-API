package com.unicar.dto.interesseTrajeto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CoordenadaDTO(

        @NotNull(message = "Latitude é obrigatória")
        BigDecimal latitude,

        @NotNull(message = "Longitude é obrigatória")
        BigDecimal longitude
) {}
