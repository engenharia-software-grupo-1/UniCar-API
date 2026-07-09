package com.unicar.controller.carona;

import com.unicar.domain.Usuario;
import com.unicar.dto.carona.PassageiroResponseDTO;
import com.unicar.security.JwtAuthenticationFilter;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.carona.CaronaService;

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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CaronaController.class)
@AutoConfigureMockMvc(addFilters = false)
class CaronaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CaronaService caronaService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final Long USUARIO_ID = 1L;
    private static final Long CARONA_ID = 10L;

    @BeforeEach
    void setup() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setEmail("teste@email.com");

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
    @DisplayName("GET /caronas/{id}/passageiros")
    class ListarPassageiros {

        @Test
        @DisplayName("deve retornar a lista de passageiros")
        void deveListarPassageiros() throws Exception {

            List<PassageiroResponseDTO> passageiros = List.of(
                    new PassageiroResponseDTO(
                            1L,
                            USUARIO_ID,
                            "João",
                            5
                    )
            );

            when(caronaService.listarPassageiros(CARONA_ID, USUARIO_ID))
                    .thenReturn(passageiros);

            mockMvc.perform(get("/caronas/{id}/passageiros", CARONA_ID))
					.andExpect(status().isOk())
					.andExpect(content().contentType(MediaType.APPLICATION_JSON))
					.andExpect(jsonPath("$[0].reservaId").value(1))
					.andExpect(jsonPath("$[0].usuarioId").value(USUARIO_ID))
					.andExpect(jsonPath("$[0].nome").value("João"))
					.andExpect(jsonPath("$[0].quantidadePassageiros").value(5));

			verify(caronaService).listarPassageiros(CARONA_ID, USUARIO_ID);
        }
    }

    @Nested
    @DisplayName("PATCH /caronas/{id}/iniciar")
    class IniciarCarona {

        @Test
        @DisplayName("deve retornar 204 ao iniciar uma carona")
        void deveIniciarCarona() throws Exception {

            mockMvc.perform(patch("/caronas/{id}/iniciar", CARONA_ID))
                    .andExpect(status().isNoContent());

            verify(caronaService).iniciarCarona(CARONA_ID, USUARIO_ID);
        }
    }

    @Nested
    @DisplayName("PATCH /caronas/{id}/finalizar")
    class FinalizarCarona {

        @Test
        @DisplayName("deve retornar 204 ao finalizar uma carona")
        void deveFinalizarCarona() throws Exception {

            mockMvc.perform(patch("/caronas/{id}/finalizar", CARONA_ID))
                    .andExpect(status().isNoContent());

            verify(caronaService).finalizarCarona(CARONA_ID, USUARIO_ID);
        }
    }
}