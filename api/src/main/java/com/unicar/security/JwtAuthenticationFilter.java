package com.unicar.security;

import com.unicar.domain.Usuario;
import com.unicar.repository.UsuarioRepository;
import com.unicar.service.auth.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(BEARER_PREFIX.length());
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            autenticarComToken(token, request);
        }

        filterChain.doFilter(request, response);
    }

    private void autenticarComToken(String token, HttpServletRequest request) {
        try {
            if (!jwtService.tokenValido(token)) {
                return;
            }

            Long usuarioId = jwtService.extrairUsuarioId(token);
            usuarioRepository.findById(usuarioId)
                .filter(usuario -> Boolean.TRUE.equals(usuario.getAtivo()))
                .ifPresent(usuario -> autenticarUsuario(usuario, request));
        } catch (RuntimeException ignored) {
            SecurityContextHolder.clearContext();
        }
    }

    private void autenticarUsuario(Usuario usuario, HttpServletRequest request) {
        UsuarioDetails usuarioDetails = new UsuarioDetails(usuario);

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                usuarioDetails,
                null,
                usuarioDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
