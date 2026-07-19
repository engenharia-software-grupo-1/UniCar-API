package com.unicar.integration;

import com.unicar.domain.Usuario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercita a cadeia real de segurança (JwtAuthenticationFilter + BlacklistService
 * + H2), sem nenhum mock — diferente de SecurityConfigTest, que mocka o próprio
 * filtro JWT para testar só as regras de autorização por rota.
 */
class AutenticacaoJwtIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("Deve retornar 401 quando a requisição não tem token")
    void semTokenDeveRetornar401() throws Exception {
        mockMvc.perform(get("/veiculos"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"message\":\"Usuário não autenticado\"}"));
    }

    @Test
    @DisplayName("Deve autenticar com um token real gerado para um usuário persistido")
    void tokenValidoDeveAutenticar() throws Exception {
        Usuario usuario = criarUsuario("motorista");

        mockMvc.perform(get("/veiculos").header("Authorization", bearerToken(usuario)))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("Deve retornar 401 quando o token é malformado ou tem assinatura inválida")
    void tokenMalformadoDeveRetornar401() throws Exception {
        mockMvc.perform(get("/veiculos").header("Authorization", "Bearer token-invalido-123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve revogar o token no logout e bloquear reutilização em rota protegida")
    void logoutDeveRevogarTokenEBloquearReutilizacao() throws Exception {
        Usuario usuario = criarUsuario("passageiro");
        String token = bearerToken(usuario);

        mockMvc.perform(get("/veiculos").header("Authorization", token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/logout").header("Authorization", token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/veiculos").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }
}
