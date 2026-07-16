package com.unicar.service;

import com.unicar.domain.BloqueioUsuario;
import com.unicar.domain.Usuario;
import com.unicar.dto.bloqueio.BloqueioDTO;
import com.unicar.dto.bloqueio.UsuarioBloqueadoDTO;
import com.unicar.repository.BloqueioUsuarioRepository;
import com.unicar.repository.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BloqueioUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final BloqueioUsuarioRepository repository;

    @Transactional(readOnly = true)
    public List<UsuarioBloqueadoDTO> listarBloqueados(Long usuarioId) {
        return repository.findAllByUsuarioId(usuarioId).stream()
                .map(UsuarioBloqueadoDTO::from)
                .toList();
    }

    public BloqueioDTO bloquear(Long usuarioId, Long usuarioBloqueadoId) {
        if (usuarioId.equals(usuarioBloqueadoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é permitido bloquear a si mesmo");
        }

        if (repository.existsByUsuarioIdAndUsuarioBloqueadoId(usuarioId, usuarioBloqueadoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário já bloqueado");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        Usuario usuarioBloqueado = usuarioRepository.findById(usuarioBloqueadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        BloqueioUsuario bloqueio = BloqueioUsuario.builder()
                .usuario(usuario)
                .usuarioBloqueado(usuarioBloqueado)
                .build();

        bloqueio = repository.save(bloqueio);

        return BloqueioDTO.from(bloqueio);
    }

    public void desbloquear(Long usuarioId, Long usuarioBloqueadoId) {
        BloqueioUsuario bloqueio = repository.findByUsuarioIdAndUsuarioBloqueadoId(usuarioId, usuarioBloqueadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bloqueio não encontrado"));

        repository.delete(bloqueio);
    }
}