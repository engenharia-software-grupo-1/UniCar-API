package com.unicar.repository;

import com.unicar.domain.TokenRevogado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface TokenRevogadoRepository extends JpaRepository<TokenRevogado, Long> {
    boolean existsByToken(String token);
    void deleteByExpiresAtBefore(Instant agora);
}