package com.unicar.dto.carona;

import com.unicar.enums.StatusCarona;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CaronaDetalheResponseDTO (
    Long id,
    EnderecoDTO origem,
    EnderecoDTO destino,
    String pontoEncontro,
    LocalDateTime dataHoraSaida,
    Integer quantidadeVagas,
    Integer vagasDisponiveis,
    BigDecimal valorContribuicao,
    StatusCarona status,
    MotoristaResumoDTO motorista,
    VeiculoResumoDTO veiculo
) {}