package com.unicar.controller.carona;

import com.unicar.dto.trajeto.TrajetoRecorrenteDTO;
import com.unicar.dto.trajeto.TrajetoRecorrenteDetalhesDTO;
import com.unicar.dto.trajeto.TrajetoRecorrenteRecriarRequestDTO;
import com.unicar.dto.trajeto.TrajetoRecorrenteRecriarResponseDTO;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.carona.TrajetoRecorrenteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/trajetos-recorrentes")
@RequiredArgsConstructor
@Tag(name = "Trajetos Recorrentes")
@SecurityRequirement(name = "bearerAuth")
public class TrajetoRecorrenteController {

    private final TrajetoRecorrenteService trajetoRecorrenteService;

    @GetMapping
    @Operation(summary = "Lista os trajetos recorrentes do motorista autenticado")
    public ResponseEntity<List<TrajetoRecorrenteDTO>> listar(@AuthenticationPrincipal UsuarioDetails usuario) {
        return ResponseEntity.ok(trajetoRecorrenteService.listar(usuario.getUsuario().getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta os detalhes de um trajeto recorrente")
    public ResponseEntity<TrajetoRecorrenteDetalhesDTO> buscar(
            @PathVariable String id,
            @AuthenticationPrincipal UsuarioDetails usuario) {

        return ResponseEntity.ok(trajetoRecorrenteService.buscar(id, usuario.getUsuario().getId()));
    }

    @PostMapping("/{id}/recriar")
    @Operation(summary = "Cria uma nova carona reutilizando origem e destino de um trajeto recorrente")
    public ResponseEntity<TrajetoRecorrenteRecriarResponseDTO> recriar(
            @PathVariable String id,
            @Valid @RequestBody TrajetoRecorrenteRecriarRequestDTO request,
            @AuthenticationPrincipal UsuarioDetails usuario) {

        TrajetoRecorrenteRecriarResponseDTO response =
                trajetoRecorrenteService.recriar(id, request, usuario.getUsuario().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
