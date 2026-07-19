package com.unicar.dto.usuario;

import com.unicar.dto.avaliacao.AvaliacaoRecebidaDTO;

import java.util.List;

public record PerfilUsuarioDTO(
        Long id,
        String nome,
        String curso,
        String genero,
        String linkFoto,
        double reputacao,
        int quantidadeAvaliacoes,
        List<AvaliacaoRecebidaDTO> avaliacoes
) {
    public PerfilUsuarioDTO(Long id,
                            String nome,
                            String curso,
                            String genero,
                            String linkFoto,
                            double reputacao,
                            int quantidadeAvaliacoes){
        this(id, nome, curso, genero, linkFoto, reputacao, quantidadeAvaliacoes, List.of());
    }
}
