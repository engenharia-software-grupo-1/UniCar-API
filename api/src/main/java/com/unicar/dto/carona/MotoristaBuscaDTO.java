package com.unicar.dto.carona;

public record MotoristaBuscaDTO(
        Long id,
        String nome,
        String genero,
        String curso,
        String linkFoto,
        Double reputacao
) {}
