package com.unicar.service.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.unicar.domain.Usuario;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;

@Service
public final class JwtService {

    private final SecretKey secretKey;
    private final long expiracaoMs;

    public JwtService(
        @Value("${unicar.jwt.secret}") String secret,
        @Value("${unicar.jwt.expiration-ms:86400000}") long expiracaoMs
    ) {
        this.secretKey = construirSecretKey(secret);
        this.expiracaoMs = expiracaoMs;
    }

    private SecretKey construirSecretKey(String secret) {
        String valor = secret == null ? "" : secret.trim();
        if (valor.isBlank()) {
            valor = "dev-secret-key-for-local-development";
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(valor);
        } catch (IllegalArgumentException ex) {
            keyBytes = valor.getBytes(StandardCharsets.UTF_8);
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Keys.hmacShaKeyFor(digest.digest(keyBytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Falha ao inicializar JWT", ex);
        }
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();

        return Jwts.builder()
            .subject(String.valueOf(usuario.getId()))
            .issuedAt(Date.from(agora))
            .expiration(Date.from(agora.plusMillis(expiracaoMs)))
            .signWith(secretKey)
            .compact();
    }

    public Long extrairUsuarioId(String token) {
        return Long.valueOf(extrairClaims(token).getSubject());
    }

    public boolean tokenValido(String token) {
        try {
            Claims claims = extrairClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extrairClaims(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public Instant extrairExpiracao(String token) {
        return extrairClaims(token).getExpiration().toInstant();
    }
}
