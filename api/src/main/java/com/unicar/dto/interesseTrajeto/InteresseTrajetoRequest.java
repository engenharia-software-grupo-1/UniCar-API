package com.unicar.dto.interesseTrajeto;

import jakarta.validation.constraints.NotNull;

public record InteresseTrajetoRequest(
    @NotNull
    CoordenadaDTO origem,

    @NotNull
    CoordenadaDTO destino
) {}