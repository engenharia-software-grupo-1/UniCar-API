package com.unicar.service;

import com.unicar.domain.Usuario;
import com.unicar.dto.usuario.PerfilUsuarioDTO;
import com.unicar.dto.usuario.UpdateFotoPerfilRequestDTO;
import com.unicar.dto.usuario.UpdatePerfilRequestDTO;
import com.unicar.dto.usuario.UsuarioDTO;
import com.unicar.repository.UsuarioRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AvaliacaoService avaliacaoService;

    public UsuarioDTO buscarPerfil(Long usuarioId) {
        return UsuarioDTO.from(buscarUsuarioAtivo(usuarioId));
    }

    public PerfilUsuarioDTO perfilPublico(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        var reputacao = avaliacaoService.buscarReputacao(id);

        return new PerfilUsuarioDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getCurso(),
                usuario.getGenero().name(),
                usuario.getLinkFoto(),
                reputacao.media(),
                reputacao.quantidadeAvaliacoes().intValue()
        );
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

        validarUsuarioAtivo(usuario);

        return usuario;
    }

    private void validarUsuarioAtivo(Usuario usuario) {
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Usuário desativado"
            );
        }
    }

    @Transactional
    public UsuarioDTO atualizarFoto(Long usuarioId, UpdateFotoPerfilRequestDTO request) {
        Usuario usuario = buscarUsuarioAtivo(usuarioId);
        usuario.setLinkFoto(request.linkFoto());

        return UsuarioDTO.from(usuarioRepository.save(usuario));
    }
}
