package com.unicar.dto.chat;

import jakarta.validation.constraints.NotBlank;

public record EnviarMensagemRequestDTO(
        @NotBlank(message = "O conteúdo da mensagem não pode estar vazio")
        String conteudo
) {}