package com.unicar.dto.carona;

import com.unicar.enums.StatusCarona;

import java.time.LocalDateTime;

public record CaronaListItemResponseDTO (
    Long id,
    EnderecoDTO origem,
    EnderecoDTO destino,
    StatusCarona status,
    LocalDateTime dataHoraSaida
) {}