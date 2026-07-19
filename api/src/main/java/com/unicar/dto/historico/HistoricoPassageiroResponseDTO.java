package com.unicar.dto.historico;

import com.unicar.enums.StatusCarona;
import java.time.LocalDateTime;

public record HistoricoPassageiroResponseDTO(
        Long reservaId,
        Long caronaId,
        String origem,
        String destino,
        String motorista,
        StatusCarona status,
        LocalDateTime dataViagem,
        Integer quantidadePassageiros
) {}