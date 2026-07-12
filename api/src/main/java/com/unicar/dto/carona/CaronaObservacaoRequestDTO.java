package com.unicar.dto.carona;

import jakarta.validation.constraints.Size;

public record CaronaObservacaoRequestDTO(
    @Size(max = 255, message = "A observação deve ter no máximo 255 caracteres")
    String observacao
) {}