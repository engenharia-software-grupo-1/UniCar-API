package com.unicar.dto.trajeto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TrajetoRecorrenteRecriarRequestDTO(
    @NotNull(message = "Veículo é obrigatório")
    Long veiculoId,

    @NotNull(message = "Data e hora de saída são obrigatórias")
    @Future(message = "A data da viagem deve ser futura")
    LocalDateTime dataHoraSaida,

    @NotNull(message = "Quantidade de vagas é obrigatória")
    @Min(value = 1, message = "A quantidade de vagas deve ser maior que zero")
    Integer quantidadeVagas,

    @NotNull(message = "Valor de contribuição é obrigatório")
    @DecimalMin(value = "0.0", message = "Valor de contribuição deve ser positivo ou zero")
    BigDecimal valorContribuicao,

    @NotBlank(message = "Ponto de encontro é obrigatório")
    String pontoEncontro
) {}
