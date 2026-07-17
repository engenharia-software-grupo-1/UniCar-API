package com.unicar.controller;

import com.unicar.domain.Usuario;
import com.unicar.dto.interesseTrajeto.CoordenadaDTO;
import com.unicar.dto.interesseTrajeto.InteresseTrajetoCriadoDTO;
import com.unicar.dto.interesseTrajeto.InteresseTrajetoDTO;
import com.unicar.security.JwtAuthenticationFilter;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.InteresseTrajetoService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InteresseTrajetoController.class)
@AutoConfigureMockMvc(addFilters = false)
class InteresseTrajetoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InteresseTrajetoService interesseTrajetoService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final Long USUARIO_ID = 1L;

    @BeforeEach
    void setup() {

        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);

        UsuarioDetails usuarioDetails = new UsuarioDetails(usuario);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        usuarioDetails,
                        null,
                        usuarioDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("POST /interesses-trajeto")
    class Cadastrar {

        @Test
        @DisplayName("deve cadastrar interesse")
        void deveCadastrar() throws Exception {

            when(interesseTrajetoService.cadastrar(eq(USUARIO_ID), any()))
                    .thenReturn(new InteresseTrajetoCriadoDTO(10L));

            String body = """
                {
                  "origem": {
                    "latitude": -7.21456,
                    "longitude": -35.90872
                  },
                  "destino": {
                    "latitude": -7.21590,
                    "longitude": -35.90950
                  }
                }
                """;

            mockMvc.perform(post("/interesses-trajeto")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(10));

            verify(interesseTrajetoService)
                    .cadastrar(eq(USUARIO_ID), any());
        }
    }

    @Nested
    @DisplayName("GET /interesses-trajeto")
    class Listar {

        @Test
        @DisplayName("deve listar interesses")
        void deveListar() throws Exception {

            when(interesseTrajetoService.listar(USUARIO_ID))
                    .thenReturn(List.of(
                            new InteresseTrajetoDTO(
                                    1L,
                                    new CoordenadaDTO(
                                            new BigDecimal("-7.21456"),
                                            new BigDecimal("-35.90872")),
                                    new CoordenadaDTO(
                                            new BigDecimal("-7.21590"),
                                            new BigDecimal("-35.90950"))
                            )
                    ));

            mockMvc.perform(get("/interesses-trajeto"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].origem.latitude").value(-7.21456))
                    .andExpect(jsonPath("$[0].origem.longitude").value(-35.90872))
                    .andExpect(jsonPath("$[0].destino.latitude").value(-7.21590))
                    .andExpect(jsonPath("$[0].destino.longitude").value(-35.90950));

            verify(interesseTrajetoService).listar(USUARIO_ID);
        }
    }

    @Nested
    @DisplayName("DELETE /interesses-trajeto/{id}")
    class Remover {

        @Test
        @DisplayName("deve remover interesse")
        void deveRemover() throws Exception {

            mockMvc.perform(delete("/interesses-trajeto/{id}", 1L))
                    .andExpect(status().isNoContent());

            verify(interesseTrajetoService)
                    .remover(USUARIO_ID, 1L);
        }
    }
}