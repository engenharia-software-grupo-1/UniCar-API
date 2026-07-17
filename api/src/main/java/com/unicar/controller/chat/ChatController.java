package com.unicar.controller.chat;

import com.unicar.dto.chat.ChatDTO;
import com.unicar.dto.chat.EnviarMensagemRequestDTO;
import com.unicar.dto.chat.MensagemDTO;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.chat.ChatService;
import com.unicar.service.chat.MensagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chats")
@Tag(name = "Chats")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ChatService chatService;
    private final MensagemService mensagemService;

    @GetMapping
    @Operation(summary = "Listar chats do usuário autenticado")
    public ResponseEntity<List<ChatDTO>> listarChats(@AuthenticationPrincipal UsuarioDetails userDetails) {
        return ResponseEntity.ok(chatService.listarChatsDoUsuario(userDetails.getUsuario().getId()));
    }

    @GetMapping("/{id}/mensagens")
    @Operation(summary = "Listar mensagens de um chat")
    public ResponseEntity<List<MensagemDTO>> listarMensagens(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioDetails userDetails
    ) {
        return ResponseEntity.ok(mensagemService.listarMensagensDoChat(id, userDetails.getUsuario().getId()));
    }

    @PostMapping("/{id}/mensagens")
    @Operation(summary = "Enviar mensagem para um chat")
    public ResponseEntity<MensagemDTO> enviarMensagem(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioDetails userDetails,
            @Valid @RequestBody EnviarMensagemRequestDTO request
    ) {
        return ResponseEntity.ok(mensagemService.enviarMensagem(id, userDetails.getUsuario().getId(), request));
    }

    @Valid
    @PatchMapping("/{id}/lidas")
    @Operation(summary = "Marcar mensagens do chat como lidas")
    public ResponseEntity<Void> marcarComoLidas(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioDetails userDetails
    ) {
        mensagemService.marcarComoLidas(id, userDetails.getUsuario().getId());
        return ResponseEntity.noContent().build();
    }
}