package com.unicar.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicar.dto.auth.LoginRequestDTO;
import com.unicar.dto.auth.LoginResponseDTO;
import com.unicar.exception.GlobalExceptionHandler;
import com.unicar.security.JwtAuthenticationFilter;
import com.unicar.service.auth.AuthService;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    class Login {

        @Test
        void deveRetornar200ComTokenQuandoCredenciaisValidas() throws Exception {
            LoginRequestDTO request = new LoginRequestDTO("joao123", "senha-secreta");
            LoginResponseDTO response = new LoginResponseDTO("jwt-token-gerado", null);

            given(authService.login(any(LoginRequestDTO.class))).willReturn(response);

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("jwt-token-gerado"));

            verify(authService).login(any(LoginRequestDTO.class));
        }

        @Test
        void deveRetornar401QuandoCredenciaisInvalidas() throws Exception {
            LoginRequestDTO request = new LoginRequestDTO("joao123", "senha-errada");

            given(authService.login(any(LoginRequestDTO.class)))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas"));

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void deveRetornar502QuandoProvedorDeIdentidadeIndisponivel() throws Exception {
            LoginRequestDTO request = new LoginRequestDTO("joao123", "senha-secreta");

            given(authService.login(any(LoginRequestDTO.class)))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Erro no provedor."));

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway());
        }

        @Test
        void deveRetornar400QuandoUsuarioEmBranco() throws Exception {
            LoginRequestDTO request = new LoginRequestDTO("", "senha-secreta");

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verify(authService, never()).login(any());
        }

        @Test
        void deveRetornar400QuandoSenhaEmBranco() throws Exception {
            LoginRequestDTO request = new LoginRequestDTO("joao123", "");

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verify(authService, never()).login(any());
        }

        @Test
        void deveRetornar400QuandoCorpoDaRequisicaoAusente() throws Exception {
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Logout {

        @Test
        void deveRetornar204QuandoTokenValido() throws Exception {
            mockMvc.perform(post("/auth/logout")
                    .header("Authorization", "Bearer token-valido"))
                .andExpect(status().isNoContent());

            verify(authService).logout("token-valido");
        }

        @Test
        void deveRemoverPrefixoBearerAntesDeChamarService() throws Exception {
            mockMvc.perform(post("/auth/logout")
                    .header("Authorization", "Bearer   token-com-espacos  "))
                .andExpect(status().isNoContent());

            verify(authService).logout("token-com-espacos");
        }

        @Test
        void deveRetornar401QuandoTokenInvalido() throws Exception {
            org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido."))
                .when(authService).logout(anyString());

            mockMvc.perform(post("/auth/logout")
                    .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void deveRetornar400QuandoHeaderAuthorizationAusente() throws Exception {
            mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isBadRequest());

            verify(authService, never()).logout(anyString());
        }
    }
}