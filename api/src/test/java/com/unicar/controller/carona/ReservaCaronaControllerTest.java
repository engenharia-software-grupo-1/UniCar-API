package com.unicar.controller.carona;

import com.unicar.domain.Usuario;
import com.unicar.security.JwtAuthenticationFilter;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.carona.ReservaCaronaService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservaCaronaController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReservaCaronaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservaCaronaService reservaCaronaService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UsuarioDetails usuarioDetails;

    @BeforeEach
    void setup() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("teste@email.com");

        usuarioDetails = new UsuarioDetails(usuario);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        usuarioDetails,
                        null,
                        usuarioDetails.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("PATCH /reservas/{id}/cancelar")
    class CancelarReserva {

        @Test
        @DisplayName("deve retornar 204 ao cancelar uma reserva")
        void deveCancelarReserva() throws Exception {

            mockMvc.perform(patch("/reservas/{id}/cancelar", 10L))
            .andExpect(status().isNoContent());

            verify(reservaCaronaService)
                    .cancelarReserva(10L, 1L);
        }
    }
}