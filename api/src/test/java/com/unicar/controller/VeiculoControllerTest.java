package com.unicar.controller;

import com.unicar.domain.Usuario;
import com.unicar.dto.veiculo.VeiculoResponseDTO;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.VeiculoService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VeiculoControllerTest {

    @Test
    void deveListarVeiculosDoUsuarioAutenticado() {
        VeiculoService veiculoService = mock(VeiculoService.class);
        VeiculoController controller = new VeiculoController(veiculoService);
        Usuario usuario = Usuario.builder().id(7L).build();
        UsuarioDetails principal = new UsuarioDetails(usuario);

        when(veiculoService.listarPorUsuario(7L)).thenReturn(List.of(
            new VeiculoResponseDTO(1L, "Onix", "ABC1234", "Prata")
        ));

        ResponseEntity<List<VeiculoResponseDTO>> response = controller.listar(principal);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).modelo()).isEqualTo("Onix");
        assertThat(response.getBody().get(0).placa()).isEqualTo("ABC1234");
    }
}
