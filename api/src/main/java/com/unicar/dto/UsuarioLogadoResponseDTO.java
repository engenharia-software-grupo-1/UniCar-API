package com.unicar.dto;

import com.unicar.domain.Usuario;
import com.unicar.enums.Genero;

import java.time.LocalDateTime;

public record UsuarioLogadoResponseDTO(
        Long id,
        String matricula,
        String nome,
        String email,
        String cpf,
        String curso,
        Genero genero,
        Boolean receberEmail,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao
) {
    public static UsuarioLogadoResponseDTO from(Usuario usuario) {
        return new UsuarioLogadoResponseDTO(
                usuario.getId(),
                usuario.getMatricula(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getCpf(),
                usuario.getCurso(),
                usuario.getGenero(),
                usuario.getReceberEmail(),
                usuario.getDataCriacao(),
                usuario.getDataAtualizacao()
        );
    }
}