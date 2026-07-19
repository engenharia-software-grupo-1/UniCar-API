package com.unicar.dto.trajeto;

import com.unicar.dto.carona.EnderecoDTO;

import java.time.LocalDateTime;

public record TrajetoRecorrenteDTO(
    String id,
    EnderecoDTO origem,
    EnderecoDTO destino,
    Integer quantidadeViagens,
    LocalDateTime ultimaUtilizacao
) {}
