package com.unicar.service;

import com.unicar.domain.Usuario;
import com.unicar.dto.UpdatePerfilRequestDTO;
import com.unicar.dto.UsuarioLogadoResponseDTO;
import com.unicar.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioLogadoResponseDTO buscarPerfil(Long id) {

        Usuario usuario = buscarUsuario(id);

        return UsuarioLogadoResponseDTO.from(usuario);
    }

    public UsuarioLogadoResponseDTO atualizarPerfil(
            Long id,
            UpdatePerfilRequestDTO request
    ) {

        Usuario usuario = buscarUsuario(id);

        if (request.genero() != null) {
            usuario.setGenero(request.genero());
        }

        if (request.receberEmail() != null) {
            usuario.setReceberEmail(request.receberEmail());
        }

        usuarioRepository.save(usuario);

        return UsuarioLogadoResponseDTO.from(usuario);
    }

    public void desativarPerfil(Long id) {

        Usuario usuario = buscarUsuario(id);

        usuario.setAtivo(false);

        usuarioRepository.save(usuario);
    }

    private Usuario buscarUsuario(Long id) {

        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }
}