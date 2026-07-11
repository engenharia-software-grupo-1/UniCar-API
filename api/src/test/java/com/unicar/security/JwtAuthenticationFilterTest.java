package com.unicar.security;

import com.unicar.domain.Usuario;
import com.unicar.repository.UsuarioRepository;
import com.unicar.service.auth.BlacklistService;
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
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveAutenticarUsuarioAtivoComBearerTokenValido() throws Exception {

        JwtService jwtService = mock(JwtService.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        BlacklistService blacklistService = mock(BlacklistService.class);

        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtService, usuarioRepository, blacklistService);

        Usuario usuario = Usuario.builder()
                .id(10L)
                .ativo(true)
                .build();

        when(jwtService.tokenValido("token"))
                .thenReturn(true);

        when(blacklistService.estaRevogado("token"))
                .thenReturn(false);

        when(jwtService.extrairUsuarioId("token"))
                .thenReturn(10L);

        when(usuarioRepository.findById(10L))
                .thenReturn(Optional.of(usuario));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNotNull();

        UsuarioDetails details =
                (UsuarioDetails) SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal();

        assertThat(details.getUsuario()).isSameAs(usuario);
    }

    @Test
    void naoDeveAutenticarSemHeaderAuthorization() throws Exception {

        JwtService jwtService = mock(JwtService.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        BlacklistService blacklistService = mock(BlacklistService.class);

        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtService, usuarioRepository, blacklistService);

        filter.doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();

        verifyNoInteractions(jwtService);
        verifyNoInteractions(usuarioRepository);
        verifyNoInteractions(blacklistService);
    }

    @Test
    void naoDeveAutenticarQuandoTokenNaoEhBearer() throws Exception {

        JwtService jwtService = mock(JwtService.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        BlacklistService blacklistService = mock(BlacklistService.class);

        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtService, usuarioRepository, blacklistService);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic abc");

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();

        verifyNoInteractions(jwtService);
    }

    @Test
    void naoDeveAutenticarQuandoTokenInvalido() throws Exception {

        JwtService jwtService = mock(JwtService.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        BlacklistService blacklistService = mock(BlacklistService.class);

        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtService, usuarioRepository, blacklistService);

        when(jwtService.tokenValido("token"))
                .thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();

        verifyNoInteractions(usuarioRepository);
        verifyNoInteractions(blacklistService);
    }

    @Test
    void naoDeveAutenticarQuandoTokenRevogado() throws Exception {

        JwtService jwtService = mock(JwtService.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        BlacklistService blacklistService = mock(BlacklistService.class);

        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtService, usuarioRepository, blacklistService);

        when(jwtService.tokenValido("token"))
                .thenReturn(true);

        when(blacklistService.estaRevogado("token"))
                .thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();

        verify(usuarioRepository, never()).findById(anyLong());
    }

    @Test
    void naoDeveAutenticarQuandoUsuarioNaoExiste() throws Exception {

        JwtService jwtService = mock(JwtService.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        BlacklistService blacklistService = mock(BlacklistService.class);

        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtService, usuarioRepository, blacklistService);

        when(jwtService.tokenValido("token"))
                .thenReturn(true);

        when(blacklistService.estaRevogado("token"))
                .thenReturn(false);

        when(jwtService.extrairUsuarioId("token"))
                .thenReturn(1L);

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();
    }

    @Test
    void naoDeveAutenticarUsuarioInativo() throws Exception {

        JwtService jwtService = mock(JwtService.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        BlacklistService blacklistService = mock(BlacklistService.class);

        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtService, usuarioRepository, blacklistService);

        Usuario usuario = Usuario.builder()
                .id(10L)
                .ativo(false)
                .build();

        when(jwtService.tokenValido("token"))
                .thenReturn(true);

        when(blacklistService.estaRevogado("token"))
                .thenReturn(false);

        when(jwtService.extrairUsuarioId("token"))
                .thenReturn(10L);

        when(usuarioRepository.findById(10L))
                .thenReturn(Optional.of(usuario));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();
    }

    @Test
    void deveLimparContextoQuandoOcorreExcecao() throws Exception {

        JwtService jwtService = mock(JwtService.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        BlacklistService blacklistService = mock(BlacklistService.class);

        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtService, usuarioRepository, blacklistService);

        when(jwtService.tokenValido("token"))
                .thenThrow(new RuntimeException("erro"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();
    }
}
