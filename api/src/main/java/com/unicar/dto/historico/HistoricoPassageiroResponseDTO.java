package com.unicar.dto.historico;

import com.unicar.enums.StatusCarona;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistoricoCaronaResponseDTO(
        Long caronaId,
        String origemDescricao,
        String destinoDescricao,
        LocalDateTime dataHoraPartida,
        String nomeOutroParticipante,
        BigDecimal valorContribuicao,
        StatusCarona status
) {}