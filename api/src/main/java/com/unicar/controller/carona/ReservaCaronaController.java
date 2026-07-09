package com.unicar.controller.carona;

import com.unicar.security.UsuarioDetails;
import com.unicar.service.carona.ReservaCaronaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/reservas")
@RequiredArgsConstructor
@Tag(name = "Reservas")
@SecurityRequirement(name = "bearerAuth")
public class ReservaCaronaController {

    private final ReservaCaronaService reservaCaronaService;


    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancela uma reserva de carona")
    public ResponseEntity<Void> cancelar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioDetails usuario) {

        reservaCaronaService.cancelarReserva(id,usuario.getUsuario().getId());
        return ResponseEntity.noContent().build();
    }
}