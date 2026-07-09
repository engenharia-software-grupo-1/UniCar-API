package com.unicar.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicar.domain.Usuario;
import com.unicar.dto.veiculo.VeiculoRequestDTO;
import com.unicar.dto.veiculo.VeiculoResponseDTO;
import com.unicar.enums.TipoVeiculo;
import com.unicar.security.JwtAuthenticationFilter;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.VeiculoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VeiculoController.class)
@AutoConfigureMockMvc(addFilters = false)
class VeiculoControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private VeiculoService veiculoService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder()
            .id(7L)
            .nome("Joao")
            .build();
        UsuarioDetails principal = new UsuarioDetails(usuario);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GET /veiculos")
    class Listar {

        @Test
        @DisplayName("deve retornar os veiculos do usuario autenticado")
        void deveListarVeiculosDoUsuarioAutenticado() throws Exception {
            List<VeiculoResponseDTO> veiculos = List.of(
                new VeiculoResponseDTO(1L, "Onix", "ABC1234", "Prata", TipoVeiculo.CARRO),
                new VeiculoResponseDTO(2L, "CG 160", "XYZ9876", "Vermelha", TipoVeiculo.MOTO)
            );

            when(veiculoService.listarPorUsuario(7L)).thenReturn(veiculos);

            mockMvc.perform(get("/veiculos"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].modelo").value("Onix"))
                .andExpect(jsonPath("$[0].placa").value("ABC1234"))
                .andExpect(jsonPath("$[0].tipoVeiculo").value("CARRO"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].tipoVeiculo").value("MOTO"));

            verify(veiculoService).listarPorUsuario(7L);
        }
    }

    @Nested
    @DisplayName("POST /veiculos")
    class Criar {

        @Test
        @DisplayName("deve criar veiculo com payload valido")
        void deveCriarVeiculo() throws Exception {
            VeiculoRequestDTO request = new VeiculoRequestDTO("Onix", "ABC1234", "Prata", TipoVeiculo.CARRO);
            VeiculoResponseDTO response = new VeiculoResponseDTO(1L, "Onix", "ABC1234", "Prata", TipoVeiculo.CARRO);

            when(veiculoService.criar(eq(7L), any(VeiculoRequestDTO.class))).thenReturn(response);

            mockMvc.perform(post("/veiculos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.modelo").value("Onix"))
                .andExpect(jsonPath("$.placa").value("ABC1234"))
                .andExpect(jsonPath("$.cor").value("Prata"))
                .andExpect(jsonPath("$.tipoVeiculo").value("CARRO"));

            verify(veiculoService).criar(eq(7L), any(VeiculoRequestDTO.class));
        }

        @Test
        @DisplayName("deve retornar 400 quando campos obrigatorios estiverem invalidos")
        void deveRetornar400QuandoPayloadInvalido() throws Exception {
            VeiculoRequestDTO request = new VeiculoRequestDTO("", "", "Prata", null);

            mockMvc.perform(post("/veiculos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados inválidos."));

            verify(veiculoService, never()).criar(any(), any());
        }
    }

    @Nested
    @DisplayName("GET /veiculos/{id}")
    class BuscarPorId {

        @Test
        @DisplayName("deve buscar veiculo por id")
        void deveBuscarVeiculoPorId() throws Exception {
            VeiculoResponseDTO response = new VeiculoResponseDTO(1L, "Onix", "ABC1234", "Prata", TipoVeiculo.CARRO);

            when(veiculoService.buscarPorId(7L, 1L)).thenReturn(response);

            mockMvc.perform(get("/veiculos/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.modelo").value("Onix"));

            verify(veiculoService).buscarPorId(7L, 1L);
        }
    }

    @Nested
    @DisplayName("PUT /veiculos/{id}")
    class Atualizar {

        @Test
        @DisplayName("deve atualizar veiculo com payload valido")
        void deveAtualizarVeiculo() throws Exception {
            VeiculoRequestDTO request = new VeiculoRequestDTO("HB20", "DEF5678", "Branco", TipoVeiculo.CARRO);
            VeiculoResponseDTO response = new VeiculoResponseDTO(1L, "HB20", "DEF5678", "Branco", TipoVeiculo.CARRO);

            when(veiculoService.atualizar(eq(7L), eq(1L), any(VeiculoRequestDTO.class))).thenReturn(response);

            mockMvc.perform(put("/veiculos/{id}", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelo").value("HB20"))
                .andExpect(jsonPath("$.placa").value("DEF5678"));

            verify(veiculoService).atualizar(eq(7L), eq(1L), any(VeiculoRequestDTO.class));
        }

        @Test
        @DisplayName("deve retornar 400 quando corpo da requisicao estiver ausente")
        void deveRetornar400QuandoCorpoAusente() throws Exception {
            mockMvc.perform(put("/veiculos/{id}", 1L)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

            verify(veiculoService, never()).atualizar(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /veiculos/{id}")
    class Excluir {

        @Test
        @DisplayName("deve excluir veiculo")
        void deveExcluirVeiculo() throws Exception {
            mockMvc.perform(delete("/veiculos/{id}", 1L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

            verify(veiculoService).excluir(7L, 1L);
        }
    }
}