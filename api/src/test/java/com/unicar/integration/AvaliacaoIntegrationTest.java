package com.unicar.integration;

import com.unicar.domain.Avaliacao;
import com.unicar.domain.Carona;
import com.unicar.domain.Usuario;
import com.unicar.domain.Veiculo;
import com.unicar.enums.StatusCarona;
import com.unicar.enums.TipoVeiculo;
import com.unicar.repository.AvaliacaoRepository;
import com.unicar.repository.CaronaRepository;
import com.unicar.repository.VeiculoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo real de /usuarios/me/avaliacoes e /usuarios/{id}/reputacao. Não existe
 * endpoint HTTP para criar uma Avaliacao (AvaliacaoService.avaliar() não é
 * exposto por nenhum controller hoje), então a fixture é semeada direto via
 * repositório e o teste valida a leitura (listagem + agregação JPQL real de
 * AvaliacaoRepository.calcularMedia/countByAvaliadoId).
 */
class AvaliacaoIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private CaronaRepository caronaRepository;

    @Autowired
    private AvaliacaoRepository avaliacaoRepository;

    @Test
    @DisplayName("Deve listar avaliações recebidas e calcular a reputação a partir de dados reais persistidos")
    void deveListarAvaliacoesEReputacaoReais() throws Exception {
        Usuario motorista = criarUsuario("motorista");
        Usuario passageiro = criarUsuario("passageiro");

        Veiculo veiculo = veiculoRepository.save(Veiculo.builder()
                .usuario(motorista)
                .modelo("Onix")
                .placa("AVA1I23")
                .tipoVeiculo(TipoVeiculo.CARRO)
                .build());

        Carona carona = caronaRepository.save(Carona.builder()
                .motorista(motorista)
                .veiculo(veiculo)
                .origemDescricao("Bodocongó")
                .origemLatitude(new BigDecimal("-7.22000000"))
                .origemLongitude(new BigDecimal("-35.91000000"))
                .destinoDescricao("UFCG")
                .destinoLatitude(new BigDecimal("-7.23000000"))
                .destinoLongitude(new BigDecimal("-35.87000000"))
                .pontoEncontroDescricao("Portaria principal")
                .dataHoraPartida(LocalDateTime.now().minusDays(1))
                .vagasTotais(4)
                .valorContribuicao(new BigDecimal("5.00"))
                .status(StatusCarona.FINALIZADA)
                .build());

        avaliacaoRepository.save(Avaliacao.builder()
                .carona(carona)
                .avaliador(passageiro)
                .avaliado(motorista)
                .nota(5)
                .comentario("Ótima viagem")
                .dataAvaliacao(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/usuarios/me/avaliacoes").header("Authorization", bearerToken(motorista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nota").value(5))
                .andExpect(jsonPath("$[0].avaliador.id").value(passageiro.getId()))
                .andExpect(jsonPath("$[0].carona.id").value(carona.getId()));

        mockMvc.perform(get("/usuarios/{id}/reputacao", motorista.getId())
                        .header("Authorization", bearerToken(passageiro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(motorista.getId()))
                .andExpect(jsonPath("$.media").value(5.0))
                .andExpect(jsonPath("$.quantidadeAvaliacoes").value(1));
    }
}
