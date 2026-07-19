package com.unicar.controller.historico;

import com.unicar.domain.Usuario;
import com.unicar.dto.historico.DetalhesHistoricoResponseDTO;
import com.unicar.dto.historico.HistoricoMotoristaResponseDTO;
import com.unicar.dto.historico.HistoricoPassageiroResponseDTO;
import com.unicar.dto.historico.ParticipanteResumoDTO;
import com.unicar.enums.StatusCarona;
import com.unicar.security.JwtAuthenticationFilter;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.historico.HistoricoService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HistoricoController.class)
@AutoConfigureMockMvc(addFilters = false)
class HistoricoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HistoricoService historicoService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final Long usuarioId = 7L;

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder().id(usuarioId).nome("Joao").build();
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
    @DisplayName("GET /historico/motorista")
    class ListarComoMotorista {

        @Test
        @DisplayName("deve retornar o histórico paginado do usuário autenticado como motorista")
        void deveListarHistoricoComoMotorista() throws Exception {
            HistoricoMotoristaResponseDTO item = new HistoricoMotoristaResponseDTO(
                    1L, "Bodocongó", "UFCG", StatusCarona.FINALIZADA, LocalDateTime.now(), 3);

            when(historicoService.listarHistoricoComoMotorista(eq(usuarioId), any()))
                    .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1));

            mockMvc.perform(get("/historico/motorista"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].caronaId").value(1))
                    .andExpect(jsonPath("$.content[0].origem").value("Bodocongó"))
                    .andExpect(jsonPath("$.content[0].totalPassageiros").value(3))
                    .andExpect(jsonPath("$.page.totalElements").value(1));

            verify(historicoService).listarHistoricoComoMotorista(eq(usuarioId), any());
        }
    }

    @Nested
    @DisplayName("GET /historico/passageiro")
    class ListarComoPassageiro {

        @Test
        @DisplayName("deve retornar o histórico paginado do usuário autenticado como passageiro")
        void deveListarHistoricoComoPassageiro() throws Exception {
            HistoricoPassageiroResponseDTO item = new HistoricoPassageiroResponseDTO(
                    10L, 1L, "Bodocongó", "UFCG", "João Motorista", StatusCarona.FINALIZADA, LocalDateTime.now(), 2);

            when(historicoService.listarHistoricoComoPassageiro(eq(usuarioId), any()))
                    .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1));

            mockMvc.perform(get("/historico/passageiro"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].reservaId").value(10))
                    .andExpect(jsonPath("$.content[0].motorista").value("João Motorista"))
                    .andExpect(jsonPath("$.page.totalElements").value(1));

            verify(historicoService).listarHistoricoComoPassageiro(eq(usuarioId), any());
        }
    }

    @Nested
    @DisplayName("GET /historico/{caronaId}")
    class ObterDetalhes {

        @Test
        @DisplayName("deve retornar os detalhes da viagem")
        void deveRetornarDetalhesDaViagem() throws Exception {
            DetalhesHistoricoResponseDTO response = new DetalhesHistoricoResponseDTO(
                    1L, "Bodocongó", "UFCG",
                    new ParticipanteResumoDTO(usuarioId, "Joao"),
                    StatusCarona.FINALIZADA,
                    LocalDateTime.now(),
                    List.of(new ParticipanteResumoDTO(20L, "Maria"))
            );

            when(historicoService.obterDetalhesViagem(1L, usuarioId)).thenReturn(response);

            mockMvc.perform(get("/historico/{caronaId}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.caronaId").value(1))
                    .andExpect(jsonPath("$.motorista.nome").value("Joao"))
                    .andExpect(jsonPath("$.passageiros[0].nome").value("Maria"));

            verify(historicoService).obterDetalhesViagem(1L, usuarioId);
        }
    }
}
