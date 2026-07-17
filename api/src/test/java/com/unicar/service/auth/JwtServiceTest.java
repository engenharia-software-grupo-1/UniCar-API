package com.unicar.service.auth;

import com.unicar.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        String secret = Base64.getEncoder().encodeToString(
                "12345678901234567890123456789012".getBytes()
        );

        jwtService = new JwtService(secret, 60_000);
    }

    @Test
    void deveGerarTokenValido() {

        Usuario usuario = Usuario.builder()
                .id(15L)
                .build();

        String token = jwtService.gerarToken(usuario);

        assertThat(token).isNotBlank();
        assertThat(jwtService.tokenValido(token)).isTrue();
    }

    @Test
    void deveExtrairUsuarioId() {

        Usuario usuario = Usuario.builder()
                .id(99L)
                .build();

        String token = jwtService.gerarToken(usuario);

        Long id = jwtService.extrairUsuarioId(token);

        assertThat(id).isEqualTo(99L);
    }

    @Test
    void deveExtrairExpiracao() {

        Usuario usuario = Usuario.builder()
                .id(1L)
                .build();

        String token = jwtService.gerarToken(usuario);

        Instant expiracao = jwtService.extrairExpiracao(token);

        assertThat(expiracao).isAfter(Instant.now());
    }

    @Test
    void deveRetornarFalseParaTokenInvalido() {

        assertThat(jwtService.tokenValido("token-invalido"))
                .isFalse();
    }

    @Test
    void deveRetornarFalseParaTokenMalFormado() {

        assertThat(jwtService.tokenValido("abc.def"))
                .isFalse();
    }

    @Test
    void deveDetectarTokenExpirado() throws InterruptedException {

        String secret = Base64.getEncoder().encodeToString(
                "12345678901234567890123456789012".getBytes()
        );

        JwtService jwtCurto = new JwtService(secret, 1);

        Usuario usuario = Usuario.builder()
                .id(5L)
                .build();

        String token = jwtCurto.gerarToken(usuario);

        Thread.sleep(20);

        assertThat(jwtCurto.tokenValido(token))
                .isFalse();
    }

}