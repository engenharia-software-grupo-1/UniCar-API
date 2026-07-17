package com.unicar.controller;

import com.unicar.dto.avaliacao.AvaliacaoRecebidaDTO;
import com.unicar.dto.avaliacao.ReputacaoDTO;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.AvaliacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Avaliações")
@SecurityRequirement(name = "bearerAuth")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    @GetMapping("/me/avaliacoes")
    @Operation(summary = "Consultar avaliações recebidas pelo usuário autenticado")
    public ResponseEntity<List<AvaliacaoRecebidaDTO>> listarAvaliacoesDoUsuarioAutenticado(
            @AuthenticationPrincipal UsuarioDetails usuario) {

        return ResponseEntity.ok(avaliacaoService.listarAvaliacoesRecebidas(usuario.getUsuario().getId()));
    }

    @GetMapping("/{id}/reputacao")
    @Operation(summary = "Consultar reputação pública de um usuário")
    public ResponseEntity<ReputacaoDTO> consultarReputacao(
            @PathVariable Long id) {

        return ResponseEntity.ok(avaliacaoService.buscarReputacao(id));
    }
}