package com.unicar.controller;

import com.unicar.dto.usuario.PerfilUsuarioDTO;
import com.unicar.dto.usuario.UpdatePerfilRequestDTO;
import com.unicar.dto.usuario.UsuarioDTO;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuários")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {
    private final UsuarioService usuarioService;

    @GetMapping("/me")
    @Operation(summary = "Consulta perfil do usuário autenticado")
    public ResponseEntity<UsuarioDTO> buscarPerfil(@AuthenticationPrincipal UsuarioDetails userDetails) {
        return ResponseEntity.ok(usuarioService.buscarPerfil(userDetails.getUsuario().getId()));
    }

    @PatchMapping("/me")
    @Operation(summary = "Atualiza perfil do usuário autenticado")
    public ResponseEntity<UsuarioDTO> atualizarPerfil(
        @AuthenticationPrincipal UsuarioDetails userDetails,
        @Valid @RequestBody UpdatePerfilRequestDTO request
    ) {
        return ResponseEntity.ok(usuarioService.atualizarPerfil(userDetails.getUsuario().getId(), request));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Desativa perfil do usuário autenticado")
    public ResponseEntity<Void> desativarPerfil(@AuthenticationPrincipal UsuarioDetails userDetails) {
        usuarioService.desativarPerfil(userDetails.getUsuario().getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/perfil-publico")
    @Operation(summary = "Lista perfil público de um usuário")
    public ResponseEntity<PerfilUsuarioDTO> perfilPublico(@PathVariable Long id, @AuthenticationPrincipal UsuarioDetails usuarioDetails){
        return ResponseEntity.ok(usuarioService.perfilPublico(id));
    }
}
