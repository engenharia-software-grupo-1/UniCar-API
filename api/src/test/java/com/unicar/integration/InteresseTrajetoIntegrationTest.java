package com.unicar.integration;

import com.unicar.domain.Usuario;
import com.unicar.repository.InteresseTrajetoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo real de /interesses-trajeto: filtro JWT real + InteresseTrajetoService
 * real + H2, incluindo a query de deduplicação (RN-BUS-12 a RN-BUS-14).
 */
class InteresseTrajetoIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private InteresseTrajetoRepository interesseTrajetoRepository;

    private Usuario usuario;
    private String token;

    private static final String REQUEST_TRAJETO = """
            {
              "origem": {"latitude": -7.2200000, "longitude": -35.9100000},
              "destino": {"latitude": -7.2300000, "longitude": -35.8700000}
            }
            """;

    @BeforeEach
    void setup() {
        usuario = criarUsuario("interessado");
        token = bearerToken(usuario);
    }

    @Test
    @DisplayName("Deve cadastrar, listar e remover um interesse de trajeto persistindo no H2")
    void deveExecutarCicloCompletoDoInteresse() throws Exception {
        String resposta = mockMvc.perform(post("/interesses-trajeto")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_TRAJETO))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number idLido = com.jayway.jsonpath.JsonPath.read(resposta, "$.id");
        Long interesseId = idLido.longValue();

        assertThat(interesseTrajetoRepository.findById(interesseId)).isPresent();

        mockMvc.perform(get("/interesses-trajeto").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].origem.latitude").value(-7.22));

        mockMvc.perform(delete("/interesses-trajeto/{id}", interesseId).header("Authorization", token))
                .andExpect(status().isNoContent());

        assertThat(interesseTrajetoRepository.findById(interesseId)).isEmpty();
    }

    @Test
    @DisplayName("Não deve permitir cadastrar o mesmo trajeto duas vezes para o mesmo usuário")
    void naoDevePermitirInteresseDuplicado() throws Exception {
        mockMvc.perform(post("/interesses-trajeto")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_TRAJETO))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/interesses-trajeto")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_TRAJETO))
                .andExpect(status().isBadRequest());

        assertThat(interesseTrajetoRepository.findByUsuarioId(usuario.getId())).hasSize(1);
    }

    @Test
    @DisplayName("Não deve remover interesse de outro usuário nem interesse inexistente")
    void naoDeveRemoverInteresseDeOutroUsuarioOuInexistente() throws Exception {
        String resposta = mockMvc.perform(post("/interesses-trajeto")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_TRAJETO))
                .andReturn().getResponse().getContentAsString();

        Number idLido = com.jayway.jsonpath.JsonPath.read(resposta, "$.id");
        Long interesseId = idLido.longValue();

        Usuario outroUsuario = criarUsuario("outro");

        mockMvc.perform(delete("/interesses-trajeto/{id}", interesseId)
                        .header("Authorization", bearerToken(outroUsuario)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/interesses-trajeto/{id}", 999_999L).header("Authorization", token))
                .andExpect(status().isNotFound());

        assertThat(interesseTrajetoRepository.findById(interesseId)).isPresent();
    }
}
