package com.unicar.service.auth;

import com.unicar.domain.TokenRevogado;
import com.unicar.repository.TokenRevogadoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlacklistService {

    private final TokenRevogadoRepository tokenRevogadoRepository;

    public void revogar(String token, Instant expiresAt) {
        if (tokenRevogadoRepository.existsByToken(token)) return;

        tokenRevogadoRepository.save(
            TokenRevogado.builder()
                .token(token)
                .expiresAt(expiresAt)
                .build()
        );
    }

    public boolean estaRevogado(String token) {
        return tokenRevogadoRepository.existsByToken(token);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void limparExpirados() {
        tokenRevogadoRepository.deleteByExpiresAtBefore(Instant.now());
        log.info("Blacklist: tokens expirados removidos.");
    }
}