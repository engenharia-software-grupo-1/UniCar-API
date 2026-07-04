package com.unicar.service;

import com.unicar.domain.Usuario;
import com.unicar.dto.usuario.UpdatePerfilRequestDTO;
import com.unicar.dto.usuario.UsuarioDTO;
import com.unicar.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDTO buscarPerfil(Long usuarioId) {
        return UsuarioDTO.from(buscarUsuarioAtivo(usuarioId));
    }

    @Transactional
    public UsuarioDTO atualizarPerfil(Long usuarioId, UpdatePerfilRequestDTO request) {
        Usuario usuario = buscarUsuarioAtivo(usuarioId);

        if (request.genero() != null) {
            usuario.setGenero(request.genero());
        }
        if (request.receberEmail() != null) {
            usuario.setReceberEmail(request.receberEmail());
        }

        return UsuarioDTO.from(usuarioRepository.save(usuario));
    }

    @Transactional
    public void desativarPerfil(Long usuarioId) {
        Usuario usuario = buscarUsuarioAtivo(usuarioId);
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    private Usuario buscarUsuarioAtivo(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Usuário não encontrado"
            ));

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Usuário desativado"
            );
        }

        return usuario;
    }
}
