package com.unicar.dto.chat;

import java.time.LocalDateTime;

public record ChatDTO(
        Long id,
        Long reservaId,
        String nomeParticipante,
        String linkFotoParticipante,
        String ultimaMensagem,
        LocalDateTime dataUltimaMensagem,
        Integer mensagensNaoLidas
) {}
