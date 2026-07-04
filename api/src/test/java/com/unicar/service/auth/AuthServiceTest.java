package com.unicar.service.auth;

import com.unicar.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private BlacklistService blacklistService;

    @Mock
    private RestClient.Builder restClientBuilder;

    @InjectMocks
    private AuthService authService;

    @Test
    void deveFazerLogoutComSucesso() {
        String token = "token-valido";

        when(jwtService.tokenValido(token)).thenReturn(true);
        when(jwtService.extrairExpiracao(token)).thenReturn(Instant.now());

        authService.logout(token);

        verify(jwtService).tokenValido(token);
        verify(jwtService).extrairExpiracao(token);
        verify(blacklistService).revogar(eq(token), any(Instant.class));
    }

    @Test
    void deveFalharLogoutComTokenInvalido() {
        String token = "token-invalido";

        when(jwtService.tokenValido(token)).thenReturn(false);

        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> authService.logout(token)
        );

        verifyNoInteractions(blacklistService);
    }
}