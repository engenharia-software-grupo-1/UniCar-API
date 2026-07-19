package com.unicar.dto.historico;

import com.unicar.enums.StatusCarona;
import java.time.LocalDateTime;
import java.util.List;

public record DetalhesHistoricoResponseDTO(
        Long caronaId,
        String origem,
        String destino,
        ParticipanteResumoDTO motorista,
        StatusCarona status,
        LocalDateTime dataViagem,
        List<ParticipanteResumoDTO> passageiros
) {}