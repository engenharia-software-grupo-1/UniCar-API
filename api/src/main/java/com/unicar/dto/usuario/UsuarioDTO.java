package com.unicar.dto.usuario;

import com.unicar.domain.Usuario;

import java.time.LocalDateTime;

public record UsuarioDTO(
    Long id,
    String matricula,
    String nome,
    String email,
    String cpf,
    String curso,
    String genero,
    Boolean receberEmail,
    LocalDateTime dataCriacao,
    LocalDateTime dataAtualizacao
) {
    public static UsuarioDTO from(Usuario usuario) {
        return new UsuarioDTO(
            usuario.getId(),
            usuario.getMatricula(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getCpf(),
            usuario.getCurso(),
            usuario.getGenero() != null ? usuario.getGenero().name() : null,
            usuario.getReceberEmail(),
            usuario.getDataCriacao(),
            usuario.getDataAtualizacao()
        );
    }
}
