package com.unicar.controller.carona;

import com.unicar.dto.carona.CaronaDetalheResponseDTO;
import com.unicar.dto.carona.CaronaListItemResponseDTO;
import com.unicar.dto.carona.CaronaObservacaoRequestDTO;
import com.unicar.dto.carona.CaronaRequestDTO;
import com.unicar.dto.carona.CaronaResponseDTO;
import com.unicar.dto.carona.PassageiroResponseDTO;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.carona.CaronaService;

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

@RestController
@RequestMapping("/caronas")
@RequiredArgsConstructor
@Tag(name = "Caronas")
@SecurityRequirement(name = "bearerAuth")
public class CaronaController {

    private final CaronaService caronaService;

    @PostMapping
    @Operation(summary = "Cria uma nova carona")
    public ResponseEntity<CaronaResponseDTO> criar(
            @Valid @RequestBody CaronaRequestDTO request,
            @AuthenticationPrincipal UsuarioDetails usuario) {

        CaronaResponseDTO response = caronaService.criar(request, usuario.getUsuario().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/minhas")
    @Operation(summary = "Lista as caronas criadas pelo motorista autenticado")
    public ResponseEntity<List<CaronaListItemResponseDTO>> listarMinhas(
            @AuthenticationPrincipal UsuarioDetails usuario) {

        return ResponseEntity.ok(caronaService.listarMinhas(usuario.getUsuario().getId()));
    }

    @PatchMapping("/{id}/observacao")
    @Operation(summary = "Atualiza a observação de uma carona")
    public ResponseEntity<CaronaResponseDTO> atualizarObservacao(
            @PathVariable Long id,
            @RequestBody @Valid CaronaObservacaoRequestDTO request,
            @AuthenticationPrincipal UsuarioDetails usuario) {

        CaronaResponseDTO response = caronaService.atualizarObservacao(id, request, usuario.getUsuario().getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta os detalhes de uma carona")
    public ResponseEntity<CaronaDetalheResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(caronaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza informações de uma carona")
    public ResponseEntity<CaronaResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody CaronaRequestDTO request,
            @AuthenticationPrincipal UsuarioDetails usuario) {

        CaronaResponseDTO response = caronaService.atualizar(id, request, usuario.getUsuario().getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancela uma carona")
    public ResponseEntity<CaronaResponseDTO> cancelar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioDetails usuario) {

        CaronaResponseDTO response = caronaService.cancelar(id, usuario.getUsuario().getId());
        return ResponseEntity.ok(response);
    }

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