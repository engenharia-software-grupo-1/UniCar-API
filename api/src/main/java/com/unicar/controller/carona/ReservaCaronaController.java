package com.unicar.controller.carona;

import com.unicar.dto.carona.ReservaDetalheResponseDTO;
import com.unicar.dto.carona.ReservaEnviadaResponseDTO;
import com.unicar.dto.carona.ReservaRecebidaResponseDTO;
import com.unicar.dto.carona.ReservaRequestDTO;
import com.unicar.dto.carona.ReservaResponseDTO;
import com.unicar.dto.carona.ReservaSimulacaoResponseDTO;
import com.unicar.dto.carona.ReservaStatusResponseDTO;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.carona.ReservaCaronaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/reservas")
@RequiredArgsConstructor
@Tag(name = "Reservas")
@SecurityRequirement(name = "bearerAuth")
public class ReservaCaronaController {

    private final ReservaCaronaService reservaCaronaService;

    @PostMapping
    @Operation(summary = "Solicita participação em uma carona")
    public ResponseEntity<ReservaResponseDTO> solicitar(
            @Valid @RequestBody ReservaRequestDTO request,
            @AuthenticationPrincipal UsuarioDetails usuario) {

        ReservaResponseDTO response = reservaCaronaService.solicitar(request, usuario.getUsuario().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/simular")
    @Operation(summary = "Simula o valor de contribuição de uma reserva")
    public ResponseEntity<ReservaSimulacaoResponseDTO> simular(
            @Valid @RequestBody ReservaRequestDTO request) {

        return ResponseEntity.ok(reservaCaronaService.simular(request));
    }

    @GetMapping("/enviadas")
    @Operation(summary = "Lista as reservas enviadas pelo usuário autenticado")
    public ResponseEntity<List<ReservaEnviadaResponseDTO>> listarEnviadas(
            @AuthenticationPrincipal UsuarioDetails usuario) {

        return ResponseEntity.ok(reservaCaronaService.listarEnviadas(usuario.getUsuario().getId()));
    }

    @GetMapping("/recebidas")
    @Operation(summary = "Lista as reservas recebidas pelo motorista autenticado")
    public ResponseEntity<List<ReservaRecebidaResponseDTO>> listarRecebidas(
            @AuthenticationPrincipal UsuarioDetails usuario) {

        return ResponseEntity.ok(reservaCaronaService.listarRecebidas(usuario.getUsuario().getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta os detalhes de uma reserva")
    public ResponseEntity<ReservaDetalheResponseDTO> buscarPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioDetails usuario) {

        return ResponseEntity.ok(reservaCaronaService.buscarDetalhe(id, usuario.getUsuario().getId()));
    }

    @PatchMapping("/{id}/remover")
    @Operation(summary = "Remove passageiro de uma carona")
    public ResponseEntity<Void> remover(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioDetails usuario) {

        reservaCaronaService.removerReserva(id,usuario.getUsuario().getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/aceitar")
    @Operation(summary = "Aceita uma solicitação de reserva")
    public ResponseEntity<ReservaStatusResponseDTO> aceitar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioDetails usuario) {

        return ResponseEntity.ok(reservaCaronaService.aceitar(id, usuario.getUsuario().getId()));
    }

    @PatchMapping("/{id}/recusar")
    @Operation(summary = "Recusa uma solicitação de reserva")
    public ResponseEntity<ReservaStatusResponseDTO> recusar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioDetails usuario) {

        return ResponseEntity.ok(reservaCaronaService.recusar(id, usuario.getUsuario().getId()));
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancela uma reserva")
    public ResponseEntity<ReservaStatusResponseDTO> cancelar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioDetails usuario) {

        return ResponseEntity.ok(reservaCaronaService.cancelar(id, usuario.getUsuario().getId()));
    }
}