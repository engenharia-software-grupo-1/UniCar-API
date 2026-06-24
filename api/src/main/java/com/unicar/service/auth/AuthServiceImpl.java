package com.unicar.service.auth;

import com.unicar.dto.auth.LoginRequestDTO;
import com.unicar.dto.auth.LoginResponseDTO;
import com.unicar.service.auth.provider.EurecaAuthProvider;
import com.unicar.service.auth.provider.SigaaAuthProvider;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final EurecaAuthProvider eurecaAuthProvider;
    private final SigaaAuthProvider sigaaAuthProvider;

    @Override
    @Transactional
    public LoginResponseDTO login(LoginRequestDTO request) {
        try {
            return eurecaAuthProvider.autenticar(request);
        } catch (ResponseStatusException ex) {
            if (deveUsarFallbackSigaa(ex)) {
                log.warn("Eureca indisponível ({}), utilizando fallback SIGAA", ex.getReason());
                return sigaaAuthProvider.autenticar(request);
            }
            throw ex;
        }
    }

    private boolean deveUsarFallbackSigaa(ResponseStatusException ex) {
        return ex.getStatusCode() == HttpStatus.BAD_GATEWAY;
    }
}
