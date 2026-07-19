package com.unicar.controller.carona;

import com.unicar.domain.Usuario;
import com.unicar.dto.carona.*;
import com.unicar.security.JwtAuthenticationFilter;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.carona.BuscaCaronaService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private BuscaCaronaService buscaCaronaService;

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
                            5,
                            new EnderecoDTO(
                                    "Rua das Flores, 123",
                                    BigDecimal.valueOf(-7.23072),
                                    BigDecimal.valueOf(-35.88172)
                            )
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

    @Nested
    @DisplayName("GET /caronas")
    class BuscarCaronasDisponiveis {

        @Test
        @DisplayName("deve retornar caronas disponíveis para o usuário autenticado")
        void deveBuscarCaronasDisponiveis() throws Exception {

            CaronaBuscaResponseDTO caronaDto = new CaronaBuscaResponseDTO(
                    1L,
                    new EnderecoDTO("Partage Shopping", new java.math.BigDecimal("-7.2349"), new java.math.BigDecimal("-35.8692")),
                    new EnderecoDTO("UFCG", new java.math.BigDecimal("-7.2145"), new java.math.BigDecimal("-35.9087")),
                    new MotoristaBuscaDTO(3L, "Jennifer", "FEMININO", "Ciência da Computação", null, 4.0),
                    java.time.LocalDateTime.parse("2026-07-22T07:30:00"),
                    4,
                    new java.math.BigDecimal("2.00")
            );

            when(buscaCaronaService.buscarCaronasDisponiveis(any(BuscaCaronaFiltroDTO.class), eq(USUARIO_ID)))
                    .thenReturn(List.of(caronaDto));

            mockMvc.perform(get("/caronas")
                            .param("origemLatitude", "-7.22850")
                            .param("origemLongitude", "-35.87120")
                            .param("dataHoraSaida", "2026-07-22T07:30:00"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].motorista.nome").value("Jennifer"))
                    .andExpect(jsonPath("$[0].motorista.reputacao").value(4.0))
                    .andExpect(jsonPath("$[0].vagasDisponiveis").value(4));

            verify(buscaCaronaService).buscarCaronasDisponiveis(any(BuscaCaronaFiltroDTO.class), eq(USUARIO_ID));
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não há caronas disponíveis")
        void deveRetornarListaVaziaQuandoNaoHaCaronas() throws Exception {

            when(buscaCaronaService.buscarCaronasDisponiveis(any(BuscaCaronaFiltroDTO.class), eq(USUARIO_ID)))
                    .thenReturn(List.of());

            mockMvc.perform(get("/caronas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }
}
