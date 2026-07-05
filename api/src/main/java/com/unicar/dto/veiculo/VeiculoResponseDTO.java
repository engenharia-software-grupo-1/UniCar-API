package com.unicar.dto.veiculo;

import com.unicar.domain.Veiculo;
import com.unicar.enums.TipoVeiculo;

public record VeiculoResponseDTO(Long id, String modelo, String placa, String cor, TipoVeiculo tipoVeiculo) {
    public static VeiculoResponseDTO from(Veiculo veiculo) {
        return new VeiculoResponseDTO(
            veiculo.getId(),
            veiculo.getModelo(),
            veiculo.getPlaca(),
            veiculo.getCor(),
            veiculo.getTipoVeiculo()
        );
    }
}
