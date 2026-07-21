package com.unicar.controller.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicar.domain.Usuario;
import com.unicar.dto.chat.ChatDTO;
import com.unicar.dto.chat.EnviarMensagemRequestDTO;
import com.unicar.dto.chat.MensagemDTO;
import com.unicar.security.JwtAuthenticationFilter;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.chat.ChatService;
import com.unicar.service.chat.MensagemService;

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

@WebMvcTest(controllers = ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private MensagemService mensagemService;

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
    @DisplayName("GET /chats")
    class ListarChats {

        @Test
        @DisplayName("deve retornar os chats do usuario autenticado")
        void deveListarChatsDoUsuarioAutenticado() throws Exception {
            List<ChatDTO> chats = List.of(
                    new ChatDTO(1L, 10L, "Maria Passageira", null, "Oi, tudo bem?", LocalDateTime.now(), 2)
            );

            when(chatService.listarChatsDoUsuario(usuarioId)).thenReturn(chats);

            mockMvc.perform(get("/chats"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].reservaId").value(10))
                    .andExpect(jsonPath("$[0].nomeParticipante").value("Maria Passageira"))
                    .andExpect(jsonPath("$[0].mensagensNaoLidas").value(2));

            verify(chatService).listarChatsDoUsuario(usuarioId);
        }
    }

    @Nested
    @DisplayName("GET /chats/{id}/mensagens")
    class ListarMensagens {

        @Test
        @DisplayName("deve retornar as mensagens do chat")
        void deveListarMensagensDoChat() throws Exception {
            List<MensagemDTO> mensagens = List.of(
                    new MensagemDTO(100L, usuarioId, "Olá!", false, LocalDateTime.now())
            );

            when(mensagemService.listarMensagensDoChat(1L, usuarioId)).thenReturn(mensagens);

            mockMvc.perform(get("/chats/{id}/mensagens", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(100))
                    .andExpect(jsonPath("$[0].conteudo").value("Olá!"))
                    .andExpect(jsonPath("$[0].lida").value(false));

            verify(mensagemService).listarMensagensDoChat(1L, usuarioId);
        }

        @Test
        @DisplayName("deve retornar 403 quando o usuario nao participa do chat")
        void deveRetornar403QuandoUsuarioNaoParticipa() throws Exception {
            when(mensagemService.listarMensagensDoChat(1L, usuarioId))
                    .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado"));

            mockMvc.perform(get("/chats/{id}/mensagens", 1L))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Acesso negado"));
        }
    }

    @Nested
    @DisplayName("POST /chats/{id}/mensagens")
    class EnviarMensagem {

        @Test
        @DisplayName("deve enviar mensagem com payload valido")
        void deveEnviarMensagem() throws Exception {
            EnviarMensagemRequestDTO request = new EnviarMensagemRequestDTO("Olá!");
            MensagemDTO response = new MensagemDTO(100L, usuarioId, "Olá!", false, LocalDateTime.now());

            when(mensagemService.enviarMensagem(eq(1L), eq(usuarioId), any(EnviarMensagemRequestDTO.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/chats/{id}/mensagens", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(100))
                    .andExpect(jsonPath("$.conteudo").value("Olá!"))
                    .andExpect(jsonPath("$.remetenteId").value(usuarioId));

            verify(mensagemService).enviarMensagem(eq(1L), eq(usuarioId), any(EnviarMensagemRequestDTO.class));
        }

        @Test
        @DisplayName("deve retornar 400 quando o conteudo estiver em branco")
        void deveRetornar400QuandoConteudoEmBranco() throws Exception {
            EnviarMensagemRequestDTO request = new EnviarMensagemRequestDTO(" ");

            mockMvc.perform(post("/chats/{id}/mensagens", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Dados inválidos."));

            verify(mensagemService, never()).enviarMensagem(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("PATCH /chats/{id}/lidas")
    class MarcarComoLidas {

        @Test
        @DisplayName("deve marcar as mensagens do chat como lidas")
        void deveMarcarComoLidas() throws Exception {
            mockMvc.perform(patch("/chats/{id}/lidas", 1L))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(mensagemService).marcarComoLidas(1L, usuarioId);
        }
    }
}
