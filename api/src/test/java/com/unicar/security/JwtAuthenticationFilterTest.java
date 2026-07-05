package com.unicar.security;

import com.unicar.domain.Usuario;
import com.unicar.repository.UsuarioRepository;

import com.unicar.service.auth.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveAutenticarUsuarioAtivoComBearerTokenValido() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, usuarioRepository);

        Usuario usuario = Usuario.builder()
            .id(10L)
            .ativo(true)
            .build();

        when(jwtService.tokenValido("token-valido")).thenReturn(true);
        when(jwtService.extrairUsuarioId("token-valido")).thenReturn(10L);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token-valido");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isInstanceOf(UsuarioDetails.class);
        assertThat(((UsuarioDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsuario()).isSameAs(usuario);
    }

    @Test
    void naoAutenticaUsuarioInativo() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, usuarioRepository);

        Usuario usuario = Usuario.builder()
            .id(10L)
            .ativo(false)
            .build();

        when(jwtService.tokenValido("token-valido")).thenReturn(true);
        when(jwtService.extrairUsuarioId("token-valido")).thenReturn(10L);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token-valido");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
