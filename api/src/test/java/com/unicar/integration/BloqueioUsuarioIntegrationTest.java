package com.unicar.integration;

import com.unicar.domain.Usuario;
import com.unicar.repository.BloqueioUsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo real de /usuarios/{id}/bloquear e /usuarios/bloqueados: filtro JWT
 * real + BloqueioUsuarioService real + H2 (unicidade do bloqueio imposta
 * pelo banco).
 */
class BloqueioUsuarioIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private BloqueioUsuarioRepository bloqueioUsuarioRepository;

    private Usuario usuario;
    private Usuario bloqueado;
    private String token;

    @BeforeEach
    void setup() {
        usuario = criarUsuario("usuario");
        bloqueado = criarUsuario("bloqueado");
        token = bearerToken(usuario);
    }

    @Test
    @DisplayName("Deve bloquear, listar e desbloquear um usuário persistindo no H2")
    void deveExecutarCicloCompletoDoBloqueio() throws Exception {
        mockMvc.perform(post("/usuarios/{id}/bloquear", bloqueado.getId()).header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(usuario.getId()))
                .andExpect(jsonPath("$.usuarioBloqueadoId").value(bloqueado.getId()));

        assertThat(bloqueioUsuarioRepository.existsByUsuarioIdAndUsuarioBloqueadoId(usuario.getId(), bloqueado.getId()))
                .isTrue();

        mockMvc.perform(get("/usuarios/bloqueados").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(bloqueado.getId()));

        mockMvc.perform(delete("/usuarios/{id}/bloquear", bloqueado.getId()).header("Authorization", token))
                .andExpect(status().isNoContent());

        assertThat(bloqueioUsuarioRepository.existsByUsuarioIdAndUsuarioBloqueadoId(usuario.getId(), bloqueado.getId()))
                .isFalse();
    }

    @Test
    @DisplayName("Não deve permitir bloquear o mesmo usuário duas vezes nem bloquear a si mesmo")
    void naoDevePermitirBloqueioDuplicadoOuAutoBloqueio() throws Exception {
        mockMvc.perform(post("/usuarios/{id}/bloquear", bloqueado.getId()).header("Authorization", token))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/usuarios/{id}/bloquear", bloqueado.getId()).header("Authorization", token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/usuarios/{id}/bloquear", usuario.getId()).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 404 ao bloquear ou desbloquear usuário inexistente")
    void deveRetornar404ParaUsuarioInexistente() throws Exception {
        mockMvc.perform(post("/usuarios/{id}/bloquear", 999_999L).header("Authorization", token))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/usuarios/{id}/bloquear", 999_999L).header("Authorization", token))
                .andExpect(status().isNotFound());
    }
}
