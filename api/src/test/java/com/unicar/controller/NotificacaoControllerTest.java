package com.unicar.controller;

import com.unicar.domain.Usuario;
import com.unicar.dto.notificacao.ContadorNotificacoesDTO;
import com.unicar.dto.notificacao.NotificacaoDTO;
import com.unicar.security.JwtAuthenticationFilter;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.NotificacaoService;

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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificacaoController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificacaoService notificacaoService;

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
    @DisplayName("GET /notificacoes")
    class ListarNotificacoes {

        @Test
        @DisplayName("deve retornar as notificacoes do usuario autenticado")
        void deveListarNotificacoesDoUsuarioAutenticado() throws Exception {
            List<NotificacaoDTO> notificacoes = List.of(
                    new NotificacaoDTO(1L, "Reserva Aceita", "Sua reserva foi aceita", "RESERVA_ACEITA", false, LocalDateTime.now())
            );

            when(notificacaoService.listarNotificacoesDoUsuario(usuarioId)).thenReturn(notificacoes);

            mockMvc.perform(get("/notificacoes"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].titulo").value("Reserva Aceita"))
                    .andExpect(jsonPath("$[0].tipo").value("RESERVA_ACEITA"))
                    .andExpect(jsonPath("$[0].visualizada").value(false));

            verify(notificacaoService).listarNotificacoesDoUsuario(usuarioId);
        }
    }

    @Nested
    @DisplayName("GET /notificacoes/contador")
    class ObterContador {

        @Test
        @DisplayName("deve retornar a quantidade de notificacoes nao lidas")
        void deveRetornarContadorDeNaoLidas() throws Exception {
            when(notificacaoService.obterContadorNaoLidas(usuarioId)).thenReturn(new ContadorNotificacoesDTO(3));

            mockMvc.perform(get("/notificacoes/contador"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.quantidadeNaoLidas").value(3));

            verify(notificacaoService).obterContadorNaoLidas(usuarioId);
        }
    }

    @Nested
    @DisplayName("PATCH /notificacoes/{id}/visualizar")
    class MarcarComoVisualizada {

        @Test
        @DisplayName("deve marcar a notificacao como visualizada")
        void deveMarcarComoVisualizada() throws Exception {
            mockMvc.perform(patch("/notificacoes/{id}/visualizar", 1L))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(notificacaoService).marcarComoVisualizada(1L, usuarioId);
        }

        @Test
        @DisplayName("deve retornar 404 quando a notificacao nao existir")
        void deveRetornar404QuandoNotificacaoNaoExistir() throws Exception {
            doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificação não encontrada"))
                    .when(notificacaoService).marcarComoVisualizada(1L, usuarioId);

            mockMvc.perform(patch("/notificacoes/{id}/visualizar", 1L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Notificação não encontrada"));
        }

        @Test
        @DisplayName("deve retornar 403 quando a notificacao nao pertencer ao usuario autenticado")
        void deveRetornar403QuandoNotificacaoNaoPertence() throws Exception {
            doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: Esta notificação não pertence a você"))
                    .when(notificacaoService).marcarComoVisualizada(1L, usuarioId);

            mockMvc.perform(patch("/notificacoes/{id}/visualizar", 1L))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Acesso negado: Esta notificação não pertence a você"));
        }
    }
}
