package com.unicar.dto.avaliacao;

import java.util.List;

public record ReputacaoDTO(
        Long usuarioId,
        Double media,
        Long quantidadeAvaliacoes,
        List<AvaliacaoRecebidaDTO> avaliacoes
) {

    public ReputacaoDTO(Long usuarioId, Double media, Long quantidadeAvaliacoes) {
        this(usuarioId, media, quantidadeAvaliacoes, List.of());
    }
}