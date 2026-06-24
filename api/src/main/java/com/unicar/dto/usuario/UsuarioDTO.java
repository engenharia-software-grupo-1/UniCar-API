package com.unicar.dto.usuario;

import com.unicar.domain.Usuario;
import com.unicar.util.PerfilPendenteUtil;

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
    Boolean perfilCompleto,
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
            PerfilPendenteUtil.perfilCompleto(usuario),
            usuario.getDataCriacao(),
            usuario.getDataAtualizacao()
        );
    }
}
