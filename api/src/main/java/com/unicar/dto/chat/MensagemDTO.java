package com.unicar.dto.chat;

import com.unicar.domain.chat.Mensagem;
import java.time.LocalDateTime;

public record MensagemDTO(
        Long id,
        Long remetenteId,
        String conteudo,
        Boolean lida,
        LocalDateTime dataEnvio
) {
    public static MensagemDTO from(Mensagem mensagem) {
        return new MensagemDTO(
                mensagem.getId(),
                mensagem.getRemetente().getId(),
                mensagem.getConteudo(),
                mensagem.getLida(),
                mensagem.getDataEnvio()
        );
    }
}