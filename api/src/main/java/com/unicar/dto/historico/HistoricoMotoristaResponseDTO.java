package com.unicar.dto.historico;

import com.unicar.enums.StatusCarona;
import java.time.LocalDateTime;

public record HistoricoMotoristaResponseDTO(
        Long caronaId,
        String origem,
        String destino,
        StatusCarona status,
        LocalDateTime dataViagem,
        Integer totalPassageiros
) {}