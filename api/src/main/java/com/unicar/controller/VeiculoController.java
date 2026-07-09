package com.unicar.controller;

import com.unicar.dto.veiculo.VeiculoRequestDTO;
import com.unicar.dto.veiculo.VeiculoResponseDTO;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.VeiculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/veiculos")
@Tag(name = "Veículos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class VeiculoController {

    private final VeiculoService veiculoService;

    @GetMapping
    @Operation(summary = "Lista os veículos do usuário autenticado")
    public ResponseEntity<List<VeiculoResponseDTO>> listar(@AuthenticationPrincipal UsuarioDetails userDetails) {
        return ResponseEntity.ok(veiculoService.listarPorUsuario(userDetails.getUsuario()));
    }

    @PostMapping
    @Operation(summary = "Cadastra um novo veículo")
    public ResponseEntity<VeiculoResponseDTO> criar(
        @AuthenticationPrincipal UsuarioDetails userDetails,
        @Valid @RequestBody VeiculoRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(veiculoService.criar(userDetails.getUsuario(), request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um veículo do usuário autenticado")
    public ResponseEntity<VeiculoResponseDTO> buscarPorId(
        @AuthenticationPrincipal UsuarioDetails userDetails,
        @PathVariable Long id
    ) {
        return ResponseEntity.ok(veiculoService.buscarPorId(userDetails.getUsuario(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um veículo do usuário autenticado")
    public ResponseEntity<VeiculoResponseDTO> atualizar(
        @AuthenticationPrincipal UsuarioDetails userDetails,
        @PathVariable Long id,
        @Valid @RequestBody VeiculoRequestDTO request
    ) {
        return ResponseEntity.ok(veiculoService.atualizar(userDetails.getUsuario(), id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um veículo do usuário autenticado")
    public void excluir(@AuthenticationPrincipal UsuarioDetails userDetails, @PathVariable Long id) {
        veiculoService.excluir(userDetails.getUsuario(), id);
    }
}
