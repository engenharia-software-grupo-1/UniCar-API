package com.unicar.dto.carona;

import com.unicar.enums.Genero;

import java.math.BigDecimal;

public record BuscaCaronaFiltroDTO(
        BigDecimal origemLatitude,
        BigDecimal origemLongitude,
        BigDecimal destinoLatitude,
        BigDecimal destinoLongitude,
        String generoMotorista,
        String cursoMotorista,
        Double raioKm
) {}