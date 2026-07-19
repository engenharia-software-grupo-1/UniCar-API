package com.unicar.controller.historico;

import com.unicar.dto.historico.DetalhesHistoricoResponseDTO;
import com.unicar.dto.historico.HistoricoMotoristaResponseDTO;
import com.unicar.dto.historico.HistoricoPassageiroResponseDTO;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.historico.HistoricoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/historico")
@RequiredArgsConstructor
@Tag(name = "Histórico", description = "Endpoints para consulta de histórico de caronas finalizadas")
@SecurityRequirement(name = "bearerAuth")
public class HistoricoController {

    private final HistoricoService historicoService;

    @GetMapping("/motorista")
    @Operation(summary = "Consulta o histórico de caronas do usuário autenticado como motorista")
    public ResponseEntity<PagedModel<HistoricoMotoristaResponseDTO>> listarComoMotorista(
            @AuthenticationPrincipal UsuarioDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<HistoricoMotoristaResponseDTO> pagina = historicoService.listarHistoricoComoMotorista(
                userDetails.getUsuario().getId(),
                pageable
        );
        return ResponseEntity.ok(new PagedModel<>(pagina));
    }

    @GetMapping("/passageiro")
    @Operation(summary = "Consulta o histórico de caronas do usuário autenticado como passageiro")
    public ResponseEntity<PagedModel<HistoricoPassageiroResponseDTO>> listarComoPassageiro(
            @AuthenticationPrincipal UsuarioDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<HistoricoPassageiroResponseDTO> pagina = historicoService.listarHistoricoComoPassageiro(
                userDetails.getUsuario().getId(),
                pageable
        );
        return ResponseEntity.ok(new PagedModel<>(pagina));
    }

    @GetMapping("/{caronaId}")
    @Operation(summary = "Consulta detalhes específicos de uma carona do histórico")
    public ResponseEntity<DetalhesHistoricoResponseDTO> obterDetalhes(
            @PathVariable Long caronaId,
            @AuthenticationPrincipal UsuarioDetails userDetails
    ) {
        return ResponseEntity.ok(historicoService.obterDetalhesViagem(
                caronaId,
                userDetails.getUsuario().getId()
        ));
    }
}