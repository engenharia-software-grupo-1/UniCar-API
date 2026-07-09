package com.unicar.controller.carona;

import com.unicar.dto.carona.PassageiroResponseDTO;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.carona.CaronaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/caronas")
@RequiredArgsConstructor
@Tag(name = "Caronas")
@SecurityRequirement(name = "bearerAuth")
public class CaronaController {

    private final CaronaService caronaService;

    @GetMapping("/{id}/passageiros")
    @Operation(summary = "Lista os passageiros de uma carona")
    public ResponseEntity<List<PassageiroResponseDTO>> listarPassageiros(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioDetails usuario) {

        return ResponseEntity.ok(caronaService.listarPassageiros(id,usuario.getUsuario().getId()));
    }

    @PatchMapping("/{id}/iniciar")
    @Operation(summary = "Inicia uma carona")
    public ResponseEntity<Void> iniciar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioDetails usuario) {

        caronaService.iniciarCarona(id,usuario.getUsuario().getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/finalizar")
    @Operation(summary = "Finaliza uma carona")
    public ResponseEntity<Void> finalizar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioDetails usuario) {

        caronaService.finalizarCarona(id,usuario.getUsuario().getId());

        return ResponseEntity.noContent().build();
    }
}