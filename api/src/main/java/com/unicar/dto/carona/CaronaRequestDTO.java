package com.unicar.dto.carona;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record CaronaRequestDTO(

    @NotNull(message = "Veículo é obrigatório")
    Long veiculoId,

    @NotNull(message = "Origem é obrigatória")
    @Valid
    EnderecoDTO origem,

    @NotNull(message = "Destino é obrigatório")
    @Valid
    EnderecoDTO destino,

    @NotBlank(message = "Ponto de encontro é obrigatório")
    String pontoEncontro,

    @NotEmpty(message = "Pelo menos uma data e hora de saída deve ser informada")
    List<@NotNull(message = "Data de saída inválida") @Future(message = "Todas as datas das viagens devem ser futuras") LocalDateTime> datasHorasSaida,

    @NotNull(message = "Quantidade de vagas é obrigatória")
    @Min(value = 1, message = "A quantidade de vagas deve ser maior que zero")
    Integer quantidadeVagas,

    @NotNull(message = "Valor de contribuição é obrigatório")
    @DecimalMin(value = "0.0", message = "Valor de contribuição deve ser positivo ou zero")
    BigDecimal valorContribuicao,

    @Size(max = 255, message = "A observação deve ter no máximo 255 caracteres")
    String observacao
) {
    public CaronaRequestDTO {
        // Não usa List.copyOf: a lista pode legitimamente conter elementos nulos
        // até a validação de bean (@NotNull por elemento) rodar e reportar o erro
        // com a mensagem correta, em vez de estourar NPE na cópia.
        datasHorasSaida = datasHorasSaida == null
                ? null
                : Collections.unmodifiableList(new ArrayList<>(datasHorasSaida));
    }
}