package com.unicar.dto.carona;

import com.unicar.enums.StatusCarona;

public record CaronaResponseDTO(
    Long id,
    StatusCarona status
) {}