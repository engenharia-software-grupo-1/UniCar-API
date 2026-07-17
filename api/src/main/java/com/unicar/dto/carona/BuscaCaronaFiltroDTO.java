package com.unicar.dto.carona;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BuscaCaronaFiltroDTO(
        BigDecimal origemLatitude,
        BigDecimal origemLongitude,
        BigDecimal destinoLatitude,
        BigDecimal destinoLongitude,
        String generoMotorista,
        String cursoMotorista,
        Double raioKm,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime dataHoraSaida
) {}