package com.unicar.dto.chat;

import com.unicar.domain.chat.Mensagem;
import java.time.LocalDateTime;

public record MensagemDTO(
        String conteudo,
        Boolean lida,
        LocalDateTime dataEnvio,
        String nomeRemetente,
        Boolean pertencenteAoUsuarioLogado
) {
    public static MensagemDTO from(Mensagem mensagem, Long usuarioAutenticadoId) {
        return new MensagemDTO(
                mensagem.getConteudo(),
                mensagem.getLida(),
                mensagem.getDataEnvio(),
                mensagem.getRemetente().getNome(),
                mensagem.getRemetente().getId().equals(usuarioAutenticadoId)
        );
    }
}