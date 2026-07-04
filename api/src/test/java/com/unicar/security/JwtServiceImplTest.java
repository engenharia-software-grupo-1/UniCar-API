package com.unicar.security;

import org.junit.jupiter.api.Test;

import com.unicar.domain.Usuario;
import com.unicar.service.auth.JwtService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceImplTest {
    private static final String SECRET = "VGVzdGUtc2VncmVkbz0tY29tLW1haXMtZGUtdHJpbnRhLWUtZG9pcy1ieXRlcw==";

    private final JwtService jwtService = new JwtService(SECRET, 86_400_000);

    @Test
    void deveGerarTokenValidoComApenasIdentificadorDoUsuario() {
        Usuario usuario = Usuario.builder()
            .id(42L)
            .cpf("12345678900")
            .nome("Jennifer Medeiros")
            .email("jennifer@ccc.ufcg.edu.br")
            .matricula("123456789")
            .build();

        String token = jwtService.gerarToken(usuario);
        String payload = new String(
            Base64.getUrlDecoder().decode(token.split("\\.")[1]),
            StandardCharsets.UTF_8
        );

        assertThat(jwtService.tokenValido(token)).isTrue();
        assertThat(jwtService.extrairUsuarioId(token)).isEqualTo(42L);
        assertThat(payload)
            .doesNotContain("cpf")
            .doesNotContain("nome")
            .doesNotContain("email")
            .doesNotContain("matricula")
            .doesNotContain("12345678900")
            .doesNotContain("jennifer@ccc.ufcg.edu.br");
    }
}
