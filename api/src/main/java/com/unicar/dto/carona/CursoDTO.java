package com.unicar.dto.carona;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CursoDTO(
        @JsonProperty("descricao") String descricao
) {
}