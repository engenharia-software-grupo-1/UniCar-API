package com.unicar.controller;

import com.unicar.dto.interesseTrajeto.InteresseTrajetoDTO;
import com.unicar.dto.interesseTrajeto.InteresseTrajetoRequest;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.InteresseTrajetoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/interesses-trajeto")
@RequiredArgsConstructor
@Tag(name = "Busca", description = "Busca de caronas disponíveis, perfis públicos e interesses")
@SecurityRequirement(name = "bearerAuth")
public class InteresseTrajetoController {

    private final InteresseTrajetoService interesseTrajetoService;

    @PostMapping
    @Operation(
            summary = "Cadastrar interesse em um trajeto",
            description = "Registra interesse do usuário em um trajeto futuro. Não permite duplicatas (RN-BUS-12 a RN-BUS-14)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Interesse cadastrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = InteresseTrajetoDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos ou interesse duplicado",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Não autenticado",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    public ResponseEntity<InteresseTrajetoDTO> cadastrar(
            @AuthenticationPrincipal UsuarioDetails userDetails,
            @Valid @RequestBody InteresseTrajetoRequest request) {

        InteresseTrajetoDTO dto = interesseTrajetoService.cadastrar(
                userDetails.getUsuario().getId(),
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    @Operation(
            summary = "Listar interesses de trajeto do usuário autenticado",
            description = "Retorna apenas interesses do usuário autenticado (RN-BUS-15)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de interesses",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(
                                    schema = @Schema(implementation = InteresseTrajetoDTO.class)
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Não autenticado",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    public ResponseEntity<List<InteresseTrajetoDTO>> listar(
            @AuthenticationPrincipal UsuarioDetails userDetails) {

        return ResponseEntity.ok(
                interesseTrajetoService.listar(userDetails.getUsuario().getId())
        );
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Remover um interesse de trajeto",
            description = "Apenas o proprietário pode remover (RN-BUS-16 e RN-BUS-17)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Interesse removido"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Interesse não encontrado",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    public ResponseEntity<Void> remover(
            @Parameter(description = "ID do interesse", example = "5")
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioDetails userDetails) {

        interesseTrajetoService.remover(
                userDetails.getUsuario().getId(),
                id
        );

        return ResponseEntity.noContent().build();
    }
}