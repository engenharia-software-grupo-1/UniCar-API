package com.unicar.controller;

import com.unicar.domain.Usuario;
import com.unicar.dto.veiculo.VeiculoRequestDTO;
import com.unicar.dto.veiculo.VeiculoResponseDTO;
import com.unicar.enums.TipoVeiculo;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.VeiculoService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VeiculoControllerTest {

    @Mock
    private VeiculoService veiculoService;

    private VeiculoController controller;
    private Usuario usuario;
    private UsuarioDetails principal;

    @BeforeEach
    void setUp() {
        veiculoService = mock(VeiculoService.class);
        controller = new VeiculoController(veiculoService);

        usuario = Usuario.builder()
            .id(7L)
            .nome("João")
            .build();

        principal = new UsuarioDetails(usuario);
    }

    @Test
    @DisplayName("Deve listar os veículos do usuário autenticado")
    void deveListarVeiculosDoUsuarioAutenticado() {
        List<VeiculoResponseDTO> veiculos = List.of(
            new VeiculoResponseDTO(1L, "Onix", "ABC1234", "Prata", TipoVeiculo.CARRO),
            new VeiculoResponseDTO(2L, "CG 160", "XYZ9876", "Vermelha", TipoVeiculo.MOTO)
        );

        when(veiculoService.listarPorUsuario(usuario)).thenReturn(veiculos);

        ResponseEntity<List<VeiculoResponseDTO>> response = controller.listar(principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<VeiculoResponseDTO> body = Objects.requireNonNull(response.getBody());

        assertThat(body).hasSize(2);

        VeiculoResponseDTO primeiro = body.get(0);
        assertThat(primeiro.id()).isEqualTo(1L);
        assertThat(primeiro.modelo()).isEqualTo("Onix");
        assertThat(primeiro.placa()).isEqualTo("ABC1234");
        assertThat(primeiro.cor()).isEqualTo("Prata");
        assertThat(primeiro.tipoVeiculo()).isEqualTo(TipoVeiculo.CARRO);

        VeiculoResponseDTO segundo = body.get(1);
        assertThat(segundo.id()).isEqualTo(2L);
        assertThat(segundo.modelo()).isEqualTo("CG 160");
        assertThat(segundo.placa()).isEqualTo("XYZ9876");
        assertThat(segundo.cor()).isEqualTo("Vermelha");
        assertThat(segundo.tipoVeiculo()).isEqualTo(TipoVeiculo.MOTO);

        verify(veiculoService).listarPorUsuario(usuario);
        verifyNoMoreInteractions(veiculoService);
    }

    @Test
    @DisplayName("Deve criar um veículo")
    void deveCriarVeiculo() {
        VeiculoRequestDTO request = new VeiculoRequestDTO(
            "Onix",
            "ABC1234",
            "Prata",
            TipoVeiculo.CARRO
        );

        VeiculoResponseDTO responseDTO = new VeiculoResponseDTO(
            1L,
            "Onix",
            "ABC1234",
            "Prata",
            TipoVeiculo.CARRO
        );

        when(veiculoService.criar(usuario, request)).thenReturn(responseDTO);

        ResponseEntity<VeiculoResponseDTO> response = controller.criar(principal, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(responseDTO);

        verify(veiculoService).criar(usuario, request);
        verifyNoMoreInteractions(veiculoService);
    }

    @Test
    @DisplayName("Deve buscar veículo por id")
    void deveBuscarVeiculoPorId() {
        VeiculoResponseDTO responseDTO = new VeiculoResponseDTO(
            1L,
            "Onix",
            "ABC1234",
            "Prata",
            TipoVeiculo.CARRO
        );

        when(veiculoService.buscarPorId(usuario, 1L)).thenReturn(responseDTO);

        ResponseEntity<VeiculoResponseDTO> response = controller.buscarPorId(principal, 1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(responseDTO);

        verify(veiculoService).buscarPorId(usuario, 1L);
        verifyNoMoreInteractions(veiculoService);
    }

    @Test
    @DisplayName("Deve atualizar veículo")
    void deveAtualizarVeiculo() {
        VeiculoRequestDTO request = new VeiculoRequestDTO(
            "HB20",
            "DEF5678",
            "Branco",
            TipoVeiculo.CARRO
        );

        VeiculoResponseDTO responseDTO = new VeiculoResponseDTO(
            1L,
            "HB20",
            "DEF5678",
            "Branco",
            TipoVeiculo.CARRO
        );

        when(veiculoService.atualizar(usuario, 1L, request)).thenReturn(responseDTO);

        ResponseEntity<VeiculoResponseDTO> response = controller.atualizar(principal, 1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(responseDTO);

        verify(veiculoService).atualizar(usuario, 1L, request);
        verifyNoMoreInteractions(veiculoService);
    }

    @Test
    @DisplayName("Deve excluir veículo")
    void deveExcluirVeiculo() {
        controller.excluir(principal, 1L);

        verify(veiculoService).excluir(usuario, 1L);
        verifyNoMoreInteractions(veiculoService);
    }
}