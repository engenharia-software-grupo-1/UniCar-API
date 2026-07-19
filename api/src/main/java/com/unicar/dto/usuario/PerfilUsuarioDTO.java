package com.unicar.dto.usuario;

public record PerfilUsuarioDTO(
        Long id,
        String nome,
        String curso,
        String genero,
        String linkFoto,
        double reputacao,
        int quantidadeAvaliacoes
) {
}
