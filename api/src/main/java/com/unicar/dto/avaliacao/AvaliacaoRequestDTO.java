package com.unicar.dto.avaliacao;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AvaliacaoRequestDTO(

        @NotNull
        Long caronaId,

        @NotNull
        Long avaliadoId,

        @NotNull
        @Min(1)
        @Max(5)
        Integer nota,

        String comentario

) {
}