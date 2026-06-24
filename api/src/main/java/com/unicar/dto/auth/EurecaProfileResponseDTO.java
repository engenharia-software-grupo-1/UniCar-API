package com.unicar.dto.auth;

import java.util.Map;

public record EurecaProfileResponseDTO(
    String id,
    String name,
    String email,
    Map<String, Object> attributes
) {
    public String matricula() {
        return atributoTexto("aluno", "matricula", "matriculaAluno", "siape");
    }

    public String curso() {
        return atributoTexto("curso", "nomeCurso", "nomeDoCurso", "programa");
    }

    private String atributoTexto(String... chaves) {
        if (attributes == null) {
            return null;
        }
        for (String chave : chaves) {
            Object valor = attributes.get(chave);
            if (valor instanceof String texto && !texto.isBlank()) {
                return texto.trim();
            }
        }
        return null;
    }
}
