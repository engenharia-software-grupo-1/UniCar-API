package com.unicar.dto.interesseTrajeto;

import java.time.LocalDateTime;

public record InteresseTrajetoDTO(
	Long id,
	CoordenadaDTO origem,
	CoordenadaDTO destino,
	LocalDateTime dataRegistro
) {
}