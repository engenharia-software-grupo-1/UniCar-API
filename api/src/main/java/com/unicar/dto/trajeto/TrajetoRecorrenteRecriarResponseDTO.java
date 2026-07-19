package com.unicar.dto.trajeto;

import com.unicar.enums.StatusCarona;

public record TrajetoRecorrenteRecriarResponseDTO(
    Long caronaId,
    StatusCarona status
) {}
