package com.unicar.integration;

import com.unicar.domain.Usuario;
import com.unicar.domain.Veiculo;
import com.unicar.enums.TipoVeiculo;
import com.unicar.repository.VeiculoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo real de /veiculos: filtro JWT real + VeiculoService real + H2,
 * validando tanto a resposta HTTP quanto o estado persistido.
 */
class VeiculoIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private VeiculoRepository veiculoRepository;

    private Usuario motorista;
    private String token;

    @BeforeEach
    void setup() {
        motorista = criarUsuario("motorista");
        token = bearerToken(motorista);
    }

    private String requestVeiculo(String placa) {
        return """
                {
                  "modelo": "Onix",
                  "placa": "__PLACA__",
                  "cor": "Prata",
                  "tipoVeiculo": "CARRO"
                }
                """.replace("__PLACA__", placa);
    }

    @Test
    @DisplayName("Deve criar, listar, buscar, atualizar e remover um veículo persistindo de verdade no H2")
    void deveExecutarCicloCompletoDoVeiculo() throws Exception {
        String resposta = mockMvc.perform(post("/veiculos")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestVeiculo("ABC1D23")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placa").value("ABC1D23"))
                .andReturn().getResponse().getContentAsString();

        Number idLido = com.jayway.jsonpath.JsonPath.read(resposta, "$.id");
        Long veiculoId = idLido.longValue();

        assertThat(veiculoRepository.findById(veiculoId)).isPresent();

        mockMvc.perform(get("/veiculos").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/veiculos/{id}", veiculoId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelo").value("Onix"));

        mockMvc.perform(put("/veiculos/{id}", veiculoId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestVeiculo("ABC1D23").replace("Onix", "HB20")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelo").value("HB20"));

        assertThat(veiculoRepository.findById(veiculoId).orElseThrow().getModelo()).isEqualTo("HB20");

        mockMvc.perform(delete("/veiculos/{id}", veiculoId).header("Authorization", token))
                .andExpect(status().isNoContent());

        assertThat(veiculoRepository.findById(veiculoId)).isEmpty();
    }

    @Test
    @DisplayName("Não deve permitir que um usuário acesse veículo de outro usuário")
    void naoDeveAcessarVeiculoDeOutroUsuario() throws Exception {
        Veiculo veiculoDoMotorista = veiculoRepository.save(Veiculo.builder()
                .usuario(motorista)
                .modelo("Gol")
                .placa("XYZ9A87")
                .tipoVeiculo(TipoVeiculo.CARRO)
                .build());

        Usuario outroUsuario = criarUsuario("outro");
        String tokenOutro = bearerToken(outroUsuario);

        mockMvc.perform(get("/veiculos/{id}", veiculoDoMotorista.getId()).header("Authorization", tokenOutro))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Não deve permitir cadastrar dois veículos com a mesma placa")
    void naoDevePermitirPlacaDuplicada() throws Exception {
        mockMvc.perform(post("/veiculos")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestVeiculo("REP3T01")))
                .andExpect(status().isCreated());

        Usuario outroUsuario = criarUsuario("segundo");

        // A violação de unicidade da placa é detectada pelo banco (constraint), não
        // pela camada de serviço, e cai no handler genérico de 500 do
        // GlobalExceptionHandler. Como isso marca a transação do teste como
        // rollback-only, nenhuma outra query pode ser feita depois nesta mesma
        // transação — por isso a asserção fica só na resposta HTTP.
        mockMvc.perform(post("/veiculos")
                        .header("Authorization", bearerToken(outroUsuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestVeiculo("REP3T01")))
                .andExpect(status().is5xxServerError());
    }
}
