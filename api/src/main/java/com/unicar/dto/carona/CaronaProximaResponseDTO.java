package com.unicar.dto.carona;

import com.unicar.enums.StatusCarona;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CaronaProximaResponseDTO(
        Long id,
        EnderecoDTO origem,
        EnderecoDTO destino,
        StatusCarona status,
        LocalDateTime dataHoraPartida,
        Integer vagasDisponiveis,
        BigDecimal distanciaKm
) {
}