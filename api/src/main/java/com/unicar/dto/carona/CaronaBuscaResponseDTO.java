package com.unicar.dto.carona;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CaronaBuscaResponseDTO(
        Long id,
        EnderecoDTO origem,
        EnderecoDTO destino,
        MotoristaBuscaDTO motorista,
        LocalDateTime dataHoraSaida,
        Integer vagasDisponiveis,
        BigDecimal valorContribuicao
) {}