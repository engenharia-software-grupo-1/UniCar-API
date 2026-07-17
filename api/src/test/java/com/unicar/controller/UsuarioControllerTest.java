package com.unicar.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import com.unicar.dto.usuario.PerfilUsuarioDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicar.domain.Usuario;
import com.unicar.dto.usuario.UpdatePerfilRequestDTO;
import com.unicar.dto.usuario.UsuarioDTO;
import com.unicar.enums.Genero;
import com.unicar.security.JwtAuthenticationFilter;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.UsuarioService;

@WebMvcTest(controllers = UsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();
 
    @MockitoBean
    private UsuarioService usuarioService;
 
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
 
    private UsuarioDetails userDetails;
 
    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .matricula("20230001")
                .nome("Oscar Rodrigues")
                .email("oscar@teste.com")
                .cpf("12345678901")
                .curso("Computação")
                .ativo(true)
                .receberEmail(true)
                .genero(Genero.NAO_INFORMADO)
                .build();
 
        userDetails = new UsuarioDetails(usuario);
 
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
 
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
 
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
 
    @Nested
    @DisplayName("GET /usuarios/me")
    class BuscarPerfil {
 
        @Test
        @DisplayName("deve retornar 200 e o perfil do usuário autenticado")
        void deveBuscarPerfil() throws Exception {
            UsuarioDTO dto = new UsuarioDTO(
                    1L, "20230001", "Oscar Rodrigues", "oscar@teste.com",
                    "12345678901", "Computação", Genero.NAO_INFORMADO.name(),
                    true, LocalDateTime.now(), LocalDateTime.now()
            );
 
            when(usuarioService.buscarPerfil(1L)).thenReturn(dto);
 
            mockMvc.perform(get("/usuarios/me"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nome").value("Oscar Rodrigues"))
                    .andExpect(jsonPath("$.cpf").value("12345678901"));
 
            verify(usuarioService).buscarPerfil(1L);
        }
    }
 
    @Nested
    @DisplayName("PATCH /usuarios/me")
    class AtualizarPerfil {
 
        @Test
        @DisplayName("deve retornar 200 e o perfil atualizado")
        void deveAtualizarPerfil() throws Exception {
            UpdatePerfilRequestDTO request = new UpdatePerfilRequestDTO(Genero.MASCULINO, false);
 
            UsuarioDTO response = new UsuarioDTO(
                    1L, "20230001", "Oscar Rodrigues", "oscar@teste.com",
                    "12345678901", "Computação", Genero.MASCULINO.name(),
                    false, LocalDateTime.now(), LocalDateTime.now()
            );
 
            when(usuarioService.atualizarPerfil(eq(1L), any(UpdatePerfilRequestDTO.class)))
                    .thenReturn(response);
 
            mockMvc.perform(patch("/usuarios/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.genero").value("MASCULINO"))
                    .andExpect(jsonPath("$.receberEmail").value(false));
 
            verify(usuarioService).atualizarPerfil(eq(1L), any(UpdatePerfilRequestDTO.class));
        }
    }
 
    @Nested
    @DisplayName("DELETE /usuarios/me")
    class DesativarPerfil {
 
        @Test
        @DisplayName("deve retornar 204 ao desativar o perfil do usuário autenticado")
        void deveDesativarPerfil() throws Exception {
            doNothing().when(usuarioService).desativarPerfil(1L);
 
            mockMvc.perform(delete("/usuarios/me"))
                    .andExpect(status().isNoContent());
 
            verify(usuarioService).desativarPerfil(1L);
        }
    }

    @Nested
    @DisplayName("GET /usuarios/{id}/perfil-publico")
    class ConsultarPerfilPublico {

        @Test
        @DisplayName("deve retornar 200 e o perfil público do usuário")
        void deveRetornarPerfilPublico() throws Exception {
            PerfilUsuarioDTO dto = new PerfilUsuarioDTO(
                    1L,
                    "Oscar Rodrigues",
                    "Computação",
                    "NAO_INFORMADO",
                    4.8,
                    15
            );

            when(usuarioService.perfilPublico(1L)).thenReturn(dto);

            mockMvc.perform(get("/usuarios/1/perfil-publico"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nome").value("Oscar Rodrigues"))
                    .andExpect(jsonPath("$.curso").value("Computação"))
                    .andExpect(jsonPath("$.genero").value("NAO_INFORMADO"))
                    .andExpect(jsonPath("$.reputacao").value(4.8))
                    .andExpect(jsonPath("$.quantidadeAvaliacoes").value(15));

            verify(usuarioService).perfilPublico(1L);
        }

        @Test
        @DisplayName("deve retornar 404 quando o id do usuário não for encontrado")
        void deveRetornar404QuandoUsuarioNaoExiste() throws Exception {
            when(usuarioService.perfilPublico(999L))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

            mockMvc.perform(get("/usuarios/999/perfil-publico"))
                    .andExpect(status().isNotFound());

            verify(usuarioService).perfilPublico(999L);
        }
    }
}