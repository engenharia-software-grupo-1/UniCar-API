package com.unicar.dto.chat;

import com.unicar.domain.chat.Chat;

public record ChatDTO(
        Long id,
        String nomeOutroParticipante,
        String destinoCarona
) {
    public static ChatDTO from(Chat chat, Long usuarioAutenticadoId) {
        Long passageiroId = chat.getReserva().getUsuario().getId();

        String nomeExibicao = usuarioAutenticadoId.equals(passageiroId)
                ? chat.getReserva().getCarona().getMotorista().getNome()
                : chat.getReserva().getUsuario().getNome();

        return new ChatDTO(
                chat.getId(),
                nomeExibicao,
                chat.getReserva().getCarona().getDestinoDescricao()
        );
    }
}