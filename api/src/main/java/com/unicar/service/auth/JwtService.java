package com.unicar.service.auth;

import com.unicar.domain.Usuario;

public interface JwtService {
    String gerarToken(Usuario usuario);
    Long extrairUsuarioId(String token);
    boolean tokenValido(String token);
}
