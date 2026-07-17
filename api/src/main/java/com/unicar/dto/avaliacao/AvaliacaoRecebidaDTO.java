package com.unicar.dto.avaliacao;

import com.unicar.domain.Avaliacao;
import java.time.LocalDateTime;

public record AvaliacaoRecebidaDTO(
        Long id,
        Integer nota,
        String comentario,
        LocalDateTime dataAvaliacao,
        AvaliadorDTO avaliador,
        CaronaResumoDTO carona

) {

    public AvaliacaoRecebidaDTO(Avaliacao avaliacao) {
        this(
                avaliacao.getId(),
                avaliacao.getNota(),
                avaliacao.getComentario(),
                avaliacao.getDataAvaliacao(),
                new AvaliadorDTO(
                        avaliacao.getAvaliador().getId(),
                        avaliacao.getAvaliador().getNome()
                ),
                new CaronaResumoDTO(
                        avaliacao.getCarona().getId()
                )
        );
    }

    public record AvaliadorDTO(
            Long id,
            String nome
    ) {}

    public record CaronaResumoDTO(
            Long id
    ) {}
}
