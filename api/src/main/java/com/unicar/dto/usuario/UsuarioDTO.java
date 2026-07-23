package com.unicar.dto.usuario;

import com.unicar.domain.Usuario;
import com.unicar.dto.avaliacao.ReputacaoDTO;

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
    String linkFoto,
    LocalDateTime dataCriacao,
    LocalDateTime dataAtualizacao,
    Double avaliacao
) {
    public static UsuarioDTO from(Usuario usuario, Double reputacao) {
        return new UsuarioDTO(
            usuario.getId(),
            usuario.getMatricula(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getCpf(),
            usuario.getCurso(),
            usuario.getGenero() != null ? usuario.getGenero().name() : null,
            usuario.getReceberEmail(),
            usuario.getLinkFoto(),
            usuario.getDataCriacao(),
            usuario.getDataAtualizacao(),
            reputacao
        );
    }

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
                usuario.getLinkFoto(),
                usuario.getDataCriacao(),
                usuario.getDataAtualizacao(),
                0.0
        );
    }
}
