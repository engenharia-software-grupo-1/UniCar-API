package com.unicar.dto.carona;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReservaRequestDTO(
    @NotNull(message = "O id da carona é obrigatório")
    Long caronaId,

    @NotNull(message = "A quantidade de passageiros é obrigatória")
    @Positive(message = "A quantidade de passageiros deve ser maior que zero")
    Integer quantidadePassageiros,

    @NotNull(message = "O local de embarque é obrigatório")
    @Valid
    EnderecoDTO origemEmbarque
) {}
