package com.unicar.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EurecaProfessorResponseDTO(
    @JsonProperty("matricula_do_docente") Long matriculaDoDocente,
    String nome,
    Integer campus,
    String email,
    String status,
    String cpf,
    Long siape,
    Integer titulacao
) {}