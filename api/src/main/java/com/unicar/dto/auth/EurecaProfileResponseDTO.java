package com.unicar.dto.auth;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record EurecaProfileResponseDTO(
    String id,
    String name,
    String email,
    String type,
    Map<String, String> attributes
) {
    public EurecaProfileResponseDTO {
        attributes = attributes == null ? null : Collections.unmodifiableMap(new HashMap<>(attributes));
    }
}
