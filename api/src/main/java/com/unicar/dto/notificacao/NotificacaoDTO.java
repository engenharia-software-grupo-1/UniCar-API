package com.unicar.dto.notificacao;

import com.unicar.domain.Notificacao;

import java.time.LocalDateTime;

public record NotificacaoDTO(
        Long id,
        String titulo,
        String mensagem,
        String tipo,
        boolean visualizada,
        LocalDateTime dataEnvio
) {
    public static NotificacaoDTO from(Notificacao notificacao) {
        return new NotificacaoDTO(
                notificacao.getId(),
                notificacao.getTitulo(),
                notificacao.getMensagem(),
                notificacao.getTipo().name(),
                notificacao.getLida(),
                notificacao.getDataCriacao()
        );
    }
}