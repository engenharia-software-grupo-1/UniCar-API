package com.unicar.dto.chat;

import java.time.LocalDateTime;

public record ChatDTO(
        Long id,
        Long reservaId,
        String nomeParticipante,
        String ultimaMensagem,
        LocalDateTime dataUltimaMensagem,
        Integer mensagensNaoLidas
) {}