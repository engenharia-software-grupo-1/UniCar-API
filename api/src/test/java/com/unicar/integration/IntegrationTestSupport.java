package com.unicar.integration;

import com.unicar.domain.Usuario;
import com.unicar.repository.UsuarioRepository;
import com.unicar.service.auth.JwtService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Base para testes de integração fim a fim: contexto Spring completo,
 * MockMvc com a cadeia de segurança real (JWT, sem mocks) e H2 em memória
 * (perfil "test", ver application-test.yml). Cada teste roda dentro de uma
 * transação revertida ao final, então as tabelas voltam ao estado anterior
 * sem limpeza manual.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class IntegrationTestSupport {

    private static final AtomicLong SEQUENCIA = new AtomicLong(1);

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected UsuarioRepository usuarioRepository;

    protected Usuario criarUsuario(String prefixo) {
        long sequencia = SEQUENCIA.getAndIncrement();

        Usuario usuario = Usuario.builder()
                .matricula(prefixo + sequencia)
                .nome("Usuário " + prefixo)
                .email(prefixo.toLowerCase() + sequencia + "@teste.unicar.edu.br")
                .cpf(String.format("%011d", sequencia))
                .build();

        return usuarioRepository.save(usuario);
    }

    protected String bearerToken(Usuario usuario) {
        return "Bearer " + jwtService.gerarToken(usuario);
    }
}
