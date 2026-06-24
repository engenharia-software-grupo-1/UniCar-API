package com.unicar.controller;

import com.unicar.domain.Usuario;
import com.unicar.dto.UpdatePerfilRequestDTO;
import com.unicar.dto.UsuarioLogadoResponseDTO;
import com.unicar.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuários")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;

    @GetMapping("/me")
    @Operation(summary = "Consulta perfil do usuário autenticado")
    public ResponseEntity<UsuarioLogadoResponseDTO> buscarPerfil(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(usuarioService.buscarPerfil(usuario.getId()));
    }

    @PatchMapping("/me")
    @Operation(summary = "Atualiza perfil do usuário autenticado")
    public ResponseEntity<UsuarioLogadoResponseDTO> atualizarPerfil(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody UpdatePerfilRequestDTO request
    ) {
        return ResponseEntity.ok(usuarioService.atualizarPerfil(usuario.getId(), request));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Desativa perfil do usuário autenticado")
    public ResponseEntity<Void> desativarPerfil(@AuthenticationPrincipal Usuario usuario) {
        usuarioService.desativarPerfil(usuario.getId());
        return ResponseEntity.noContent().build();
    }
}