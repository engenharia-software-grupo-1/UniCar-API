package com.unicar.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.unicar.enums.Genero;

@JsonIgnoreProperties(ignoreUnknown = false)
public record UpdatePerfilRequestDTO(
        Genero genero,
        Boolean receberEmail
) {}