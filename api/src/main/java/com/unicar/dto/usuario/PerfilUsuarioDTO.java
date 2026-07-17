package com.unicar.dto.usuario;

public record PerfilUsuarioDTO (
        Long id,
        String nome,
        String curso,
        String genero,
        double reputacao,
        int quantidadeAvaliacoes
) {
}
