package com.unicar.controller.carona;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicar.domain.Usuario;
import com.unicar.dto.carona.CaronaResumoDTO;
import com.unicar.dto.carona.EnderecoDTO;
import com.unicar.dto.carona.ReservaDetalheResponseDTO;
import com.unicar.dto.carona.ReservaEnviadaResponseDTO;
import com.unicar.dto.carona.ReservaRecebidaResponseDTO;
import com.unicar.dto.carona.ReservaRequestDTO;
import com.unicar.dto.carona.ReservaResponseDTO;
import com.unicar.dto.carona.ReservaSimulacaoResponseDTO;
import com.unicar.dto.carona.ReservaStatusResponseDTO;
import com.unicar.dto.carona.UsuarioResumoDTO;
import com.unicar.enums.StatusReserva;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UsuarioDetails usuarioDetails;

    private static final Long USUARIO_ID = 1L;
    private static final Long CARONA_ID = 10L;

    @BeforeEach
    void setup() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
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
    @DisplayName("POST /reservas")
    class Solicitar {

        @Test
        @DisplayName("deve retornar 201 ao solicitar uma reserva")
        void deveSolicitarReserva() throws Exception {
            ReservaRequestDTO request = new ReservaRequestDTO(
                    CARONA_ID, 2, new EnderecoDTO("Rua Aprígio Veloso", new BigDecimal("-7.22"), new BigDecimal("-35.91")));

            ReservaResponseDTO response = new ReservaResponseDTO(
                    50L, StatusReserva.PENDENTE, 2, new BigDecimal("8.00"));

            when(reservaCaronaService.solicitar(any(), eq(USUARIO_ID))).thenReturn(response);

            mockMvc.perform(post("/reservas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.quantidadePassageiros").value(2))
                .andExpect(jsonPath("$.valorContribuicao").value(8.00));

            verify(reservaCaronaService).solicitar(any(), eq(USUARIO_ID));
        }

        @Test
        @DisplayName("deve retornar 400 quando payload inválido")
        void deveRetornar400QuandoPayloadInvalido() throws Exception {
            ReservaRequestDTO request = new ReservaRequestDTO(null, null, null);

            mockMvc.perform(post("/reservas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verify(reservaCaronaService, never()).solicitar(any(), any());
        }
    }

    @Nested
    @DisplayName("POST /reservas/simular")
    class Simular {

        @Test
        @DisplayName("deve retornar 200 com o valor simulado")
        void deveSimularValor() throws Exception {
            ReservaRequestDTO request = new ReservaRequestDTO(
                    CARONA_ID, 2, new EnderecoDTO("Rua Aprígio Veloso", new BigDecimal("-7.22"), new BigDecimal("-35.91")));

            when(reservaCaronaService.simular(any()))
                    .thenReturn(new ReservaSimulacaoResponseDTO(new BigDecimal("8.00")));

            mockMvc.perform(post("/reservas/simular")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorContribuicaoEstimado").value(8.00));

            verify(reservaCaronaService).simular(any());
        }
    }

    @Nested
    @DisplayName("GET /reservas/enviadas")
    class ListarEnviadas {

        @Test
        @DisplayName("deve retornar as reservas enviadas pelo usuário autenticado")
        void deveListarEnviadas() throws Exception {
            List<ReservaEnviadaResponseDTO> reservas = List.of(
                    new ReservaEnviadaResponseDTO(
                            50L,
                            new CaronaResumoDTO(CARONA_ID, "Bodocongó", "UFCG"),
                            StatusReserva.PENDENTE,
                            2,
                            new BigDecimal("8.00"),
                            LocalDateTime.now())
            );

            when(reservaCaronaService.listarEnviadas(USUARIO_ID)).thenReturn(reservas);

            mockMvc.perform(get("/reservas/enviadas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(50))
                .andExpect(jsonPath("$[0].carona.id").value(CARONA_ID))
                .andExpect(jsonPath("$[0].status").value("PENDENTE"));

            verify(reservaCaronaService).listarEnviadas(USUARIO_ID);
        }
    }

    @Nested
    @DisplayName("GET /reservas/recebidas")
    class ListarRecebidas {

        @Test
        @DisplayName("deve retornar as reservas recebidas pelo motorista autenticado")
        void deveListarRecebidas() throws Exception {
            List<ReservaRecebidaResponseDTO> reservas = List.of(
                    new ReservaRecebidaResponseDTO(
                            50L,
                            new UsuarioResumoDTO(5L, "Maria Oliveira"),
                            new EnderecoDTO(
                                    "Rua Aprígio Veloso",
                                    new BigDecimal("-7.22"),
                                    new BigDecimal("-35.91")
                            ),
                            2,
                            new BigDecimal("8.00"),
                            StatusReserva.PENDENTE,
                            LocalDateTime.now()
                    )
            );

            when(reservaCaronaService.listarRecebidas(USUARIO_ID)).thenReturn(reservas);

            mockMvc.perform(get("/reservas/recebidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(50))
                .andExpect(jsonPath("$[0].usuario.nome").value("Maria Oliveira"))
                .andExpect(jsonPath("$[0].status").value("PENDENTE"));

            verify(reservaCaronaService).listarRecebidas(USUARIO_ID);
        }
    }

    @Nested
    @DisplayName("GET /reservas/{id}")
    class BuscarPorId {

        @Test
        @DisplayName("deve retornar os detalhes da reserva")
        void deveBuscarDetalhe() throws Exception {
            ReservaDetalheResponseDTO response = new ReservaDetalheResponseDTO(
                    50L,
                    StatusReserva.PENDENTE,
                    2,
                    new BigDecimal("8.00"),
                    new EnderecoDTO("Rua Aprígio Veloso", new BigDecimal("-7.22"), new BigDecimal("-35.91")),
                    new CaronaResumoDTO(CARONA_ID, "Bodocongó", "UFCG"), LocalDateTime.now());

            when(reservaCaronaService.buscarDetalhe(50L, USUARIO_ID)).thenReturn(response);

            mockMvc.perform(get("/reservas/{id}", 50L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.carona.id").value(CARONA_ID))
                .andExpect(jsonPath("$.origemEmbarque.descricao").value("Rua Aprígio Veloso"));

            verify(reservaCaronaService).buscarDetalhe(50L, USUARIO_ID);
        }
    }

    @Nested
    @DisplayName("PATCH /reservas/{id}/aceitar")
    class Aceitar {

        @Test
        @DisplayName("deve retornar 200 com status ACEITA")
        void deveAceitarReserva() throws Exception {
            when(reservaCaronaService.aceitar(50L, USUARIO_ID))
                    .thenReturn(new ReservaStatusResponseDTO(50L, StatusReserva.ACEITA));

            mockMvc.perform(patch("/reservas/{id}/aceitar", 50L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.status").value("ACEITA"));

            verify(reservaCaronaService).aceitar(50L, USUARIO_ID);
        }
    }

    @Nested
    @DisplayName("PATCH /reservas/{id}/recusar")
    class Recusar {

        @Test
        @DisplayName("deve retornar 200 com status RECUSADA")
        void deveRecusarReserva() throws Exception {
            when(reservaCaronaService.recusar(50L, USUARIO_ID))
                    .thenReturn(new ReservaStatusResponseDTO(50L, StatusReserva.RECUSADA));

            mockMvc.perform(patch("/reservas/{id}/recusar", 50L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.status").value("RECUSADA"));

            verify(reservaCaronaService).recusar(50L, USUARIO_ID);
        }
    }

    @Nested
    @DisplayName("PATCH /reservas/{id}/cancelar")
    class Cancelar {

        @Test
        @DisplayName("deve retornar 200 com status CANCELADA")
        void deveCancelarReserva() throws Exception {
            when(reservaCaronaService.cancelar(50L, USUARIO_ID))
                    .thenReturn(new ReservaStatusResponseDTO(50L, StatusReserva.CANCELADA));

            mockMvc.perform(patch("/reservas/{id}/cancelar", 50L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.status").value("CANCELADA"));

            verify(reservaCaronaService).cancelar(50L, USUARIO_ID);
        }
    }

    @Nested
    @DisplayName("PATCH /reservas/{id}/remover")
    class RemoverReserva {

        @Test
        @DisplayName("deve retornar 204 ao remover uma reserva")
        void deveRemoverReserva() throws Exception {

            mockMvc.perform(patch("/reservas/{id}/remover", 10L))
            .andExpect(status().isNoContent());

            verify(reservaCaronaService)
                    .removerReservaPassageiro(10L, 1L);
        }
    }
}