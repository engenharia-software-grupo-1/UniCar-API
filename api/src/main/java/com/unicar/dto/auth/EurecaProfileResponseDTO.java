package com.unicar.dto.auth;

import java.util.Map;

public record EurecaProfileResponseDTO(
    String id,
    String name,
    String email,
    String type,
    Map<String, String> attributes
) {}
