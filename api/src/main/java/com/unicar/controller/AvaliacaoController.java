package com.unicar.controller;

import com.unicar.dto.avaliacao.AvaliacaoRecebidaDTO;
import com.unicar.dto.avaliacao.AvaliacaoRequestDTO;
import com.unicar.dto.avaliacao.ParticipantePendenteDTO;
import com.unicar.dto.avaliacao.ReputacaoDTO;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.AvaliacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Avaliações")
@SecurityRequirement(name = "bearerAuth")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    @PostMapping("/avaliacoes")
    @Operation(summary = "Registrar avaliação de um participante da carona")
    public ResponseEntity<Map<String, Long>> registrarAvaliacao(
            @AuthenticationPrincipal UsuarioDetails usuario,
            @Valid @RequestBody AvaliacaoRequestDTO dto) {
        Long avaliacaoId = avaliacaoService.avaliar(usuario.getUsuario().getId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", avaliacaoId));
    }

    @GetMapping("/usuarios/me/avaliacoes")
    @Operation(summary = "Consultar avaliações recebidas pelo usuário autenticado")
    public ResponseEntity<List<AvaliacaoRecebidaDTO>> listarAvaliacoesDoUsuarioAutenticado(
            @AuthenticationPrincipal UsuarioDetails usuario) {

        return ResponseEntity.ok(avaliacaoService.listarAvaliacoesRecebidas(usuario.getUsuario().getId()));
    }

    @GetMapping("/usuarios/{id}/reputacao")
    @Operation(summary = "Consultar reputação pública de um usuário")
    public ResponseEntity<ReputacaoDTO> consultarReputacao(
            @PathVariable Long id) {

        return ResponseEntity.ok(avaliacaoService.buscarReputacao(id));
    }

    @GetMapping("/caronas/{id}/avaliacoes-pendentes")
    @Operation(summary = "Listar participantes ainda não avaliados na carona")
    public ResponseEntity<List<ParticipantePendenteDTO>> listarAvaliacoesPendentes(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioDetails usuario) {

        return ResponseEntity.ok(avaliacaoService.listarAvaliacoesPendentes(id, usuario.getUsuario().getId()));
    }
}