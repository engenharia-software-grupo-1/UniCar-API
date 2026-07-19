package com.unicar.integration;

import com.unicar.domain.Carona;
import com.unicar.domain.ReservaCarona;
import com.unicar.domain.Usuario;
import com.unicar.domain.Veiculo;
import com.unicar.enums.StatusCarona;
import com.unicar.enums.StatusReserva;
import com.unicar.enums.TipoVeiculo;
import com.unicar.repository.CaronaRepository;
import com.unicar.repository.ReservaCaronaRepository;
import com.unicar.repository.VeiculoRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke test de ponta a ponta juntando os fluxos mais críticos do domínio,
 * tudo via HTTP real (dois usuários autenticados com tokens JWT reais
 * distintos): criar carona -> reservar -> aceitar -> iniciar -> finalizar.
 * As regras de cada etapa já têm cobertura unitária isolada; aqui o valor é
 * garantir que as camadas conversam de verdade (serialização JSON real,
 * transação real, JWT real).
 */
class CaronaReservaFluxoIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private CaronaRepository caronaRepository;

    @Autowired
    private ReservaCaronaRepository reservaCaronaRepository;

    @Test
    @DisplayName("Deve executar o fluxo completo: criar carona, reservar, aceitar, iniciar e finalizar")
    void deveExecutarFluxoCompletoDeCaronaEReserva() throws Exception {
        Usuario motorista = criarUsuario("motorista");
        Usuario passageiro = criarUsuario("passageiro");
        String tokenMotorista = bearerToken(motorista);
        String tokenPassageiro = bearerToken(passageiro);

        Veiculo veiculo = veiculoRepository.save(Veiculo.builder()
                .usuario(motorista)
                .modelo("Onix")
                .placa("FLX1U23")
                .tipoVeiculo(TipoVeiculo.CARRO)
                .build());

        LocalDateTime dataPartida = LocalDateTime.now().plusMinutes(2).withNano(0);

        String requestCarona = """
                {
                  "veiculoId": %d,
                  "origem": {"descricao": "Bodocongó", "latitude": -7.2200000, "longitude": -35.9100000},
                  "destino": {"descricao": "UFCG", "latitude": -7.2300000, "longitude": -35.8700000},
                  "pontoEncontro": "Portaria principal",
                  "datasHorasSaida": ["%s"],
                  "quantidadeVagas": 2,
                  "valorContribuicao": 3.00
                }
                """.formatted(veiculo.getId(), dataPartida);

        String respostaCarona = mockMvc.perform(post("/caronas")
                        .header("Authorization", tokenMotorista)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestCarona))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].status").value("CRIADA"))
                .andReturn().getResponse().getContentAsString();

        Long caronaId = ((Number) com.jayway.jsonpath.JsonPath.read(respostaCarona, "$[0].id")).longValue();

        String requestReserva = """
                {
                  "caronaId": %d,
                  "quantidadePassageiros": 1,
                  "origemEmbarque": {"descricao": "Bodocongó", "latitude": -7.2200000, "longitude": -35.9100000}
                }
                """.formatted(caronaId);

        String respostaReserva = mockMvc.perform(post("/reservas")
                        .header("Authorization", tokenPassageiro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestReserva))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andReturn().getResponse().getContentAsString();

        Long reservaId = ((Number) com.jayway.jsonpath.JsonPath.read(respostaReserva, "$.id")).longValue();

        mockMvc.perform(patch("/reservas/{id}/aceitar", reservaId).header("Authorization", tokenMotorista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACEITA"));

        mockMvc.perform(patch("/caronas/{id}/iniciar", caronaId).header("Authorization", tokenMotorista))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/caronas/{id}/finalizar", caronaId).header("Authorization", tokenMotorista))
                .andExpect(status().isNoContent());

        Carona caronaFinal = caronaRepository.findById(caronaId).orElseThrow();
        assertThat(caronaFinal.getStatus()).isEqualTo(StatusCarona.FINALIZADA);

        ReservaCarona reservaFinal = reservaCaronaRepository.findById(reservaId).orElseThrow();
        assertThat(reservaFinal.getStatus()).isEqualTo(StatusReserva.CONCLUIDA);
    }
}
