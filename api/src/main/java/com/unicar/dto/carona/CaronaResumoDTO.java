package com.unicar.dto.carona;

import com.unicar.domain.Carona;

public record CaronaResumoDTO(
    Long id,
    String origem,
    String destino
) {
    public CaronaResumoDTO(Carona carona) {
        this(carona.getId(), carona.getOrigemDescricao(), carona.getDestinoDescricao());
    }
}
