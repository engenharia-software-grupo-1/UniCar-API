package com.unicar.controller;

import com.unicar.dto.notificacao.ContadorNotificacoesDTO;
import com.unicar.dto.notificacao.NotificacaoDTO;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.NotificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notificacoes")
@Tag(name = "Notificações")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    @GetMapping
    @Operation(summary = "Listar notificações do usuário autenticado")
    public ResponseEntity<List<NotificacaoDTO>> listarNotificacoes(
            @AuthenticationPrincipal UsuarioDetails userDetails
    ) {
        List<NotificacaoDTO> notificacoes = notificacaoService.listarNotificacoesDoUsuario(userDetails.getUsuario().getId());
        return ResponseEntity.ok(notificacoes);
    }

    @GetMapping("/contador")
    @Operation(summary = "Obter quantidade de notificações não lidas")
    public ResponseEntity<ContadorNotificacoesDTO> obterContador(
            @AuthenticationPrincipal UsuarioDetails userDetails
    ) {
        ContadorNotificacoesDTO contador = notificacaoService.obterContadorNaoLidas(userDetails.getUsuario().getId());
        return ResponseEntity.ok(contador);
    }

    @PatchMapping("/{id}/visualizar")
    @Operation(summary = "Marcar notificação como visualizada")
    public ResponseEntity<Void> marcarComoVisualizada(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioDetails userDetails
    ) {
        notificacaoService.marcarComoVisualizada(id, userDetails.getUsuario().getId());
        return ResponseEntity.noContent().build();
    }
}