package com.unicar.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EurecaEstudanteResponseDTO(
    @JsonProperty("matricula_do_estudante") String matriculaDoEstudante,
    String nome,
    @JsonProperty("nome_do_curso") String nomeDoCurso,
    String sexo,
    String cpf,
    String email,
    String situacao
) {}