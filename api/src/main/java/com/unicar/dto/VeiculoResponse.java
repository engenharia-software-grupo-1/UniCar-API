package com.unicar.dto;

public record VeiculoResponse(
        Long id,
        String modelo,
        String placa,
        String cor
) {
}
