package com.unicar.controller;

import com.unicar.domain.BloqueioUsuario;
import com.unicar.dto.bloqueio.BloqueioDTO;
import com.unicar.dto.bloqueio.UsuarioBloqueadoDTO;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.BloqueioUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Segurança", description = "Bloqueio de usuários")
@SecurityRequirement(name = "bearerAuth")
public class BloqueioUsuarioController {

    private final BloqueioUsuarioService bloqueioService;

    @GetMapping("/bloqueados")
    @Operation(
            summary = "Listar usuários bloqueados",
            description = "Retorna todos os usuários bloqueados pelo usuário autenticado (RN-BLOQ-05)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de usuários bloqueados",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = UsuarioBloqueadoDTO.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Não autenticado",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    public ResponseEntity<List<UsuarioBloqueadoDTO>> listarBloqueados(
            @AuthenticationPrincipal UsuarioDetails userDetails) {

        List<UsuarioBloqueadoDTO> bloqueados = bloqueioService.listarBloqueados(userDetails.getUsuario().getId());
        return ResponseEntity.ok(bloqueados);
    }

    @PostMapping("/{id}/bloquear")
    @Operation(
            summary = "Bloquear um usuário",
            description = "Bloqueia o usuário identificado por `id`. Usuários bloqueados não interagem entre si, não aparecem em buscas e não podem trocar mensagens (RN-BLOQ-01 a RN-BLOQ-03)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Bloqueio registrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BloqueioDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Usuário já bloqueado ou tentativa de bloquear a si mesmo",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    public ResponseEntity<BloqueioDTO> bloquear(
            @Parameter(description = "ID do usuário a ser bloqueado", example = "5")
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioDetails userDetails) {

        BloqueioDTO novoBloqueio = bloqueioService.bloquear(userDetails.getUsuario().getId(), id);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(novoBloqueio);
    }

    @DeleteMapping("/{id}/bloquear")
    @Operation(
            summary = "Remover bloqueio de um usuário",
            description = "Remove o bloqueio previamente criado (RN-BLOQ-04)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Bloqueio removido"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Bloqueio não encontrado",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    public ResponseEntity<Void> desbloquear(
            @Parameter(description = "ID do usuário a ser desbloqueado", example = "5")
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioDetails userDetails) {

        bloqueioService.desbloquear(userDetails.getUsuario().getId(), id);
        return ResponseEntity.noContent().build();
    }
}