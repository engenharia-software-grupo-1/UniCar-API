package com.unicar.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record UpdateFotoPerfilRequestDTO(
        @NotBlank
        @URL
        String linkFoto
) {}