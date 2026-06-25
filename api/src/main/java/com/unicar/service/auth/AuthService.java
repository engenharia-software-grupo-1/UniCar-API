package com.unicar.service.auth;

import com.unicar.domain.Usuario;
import com.unicar.dto.auth.EurecaProfileResponseDTO;
import com.unicar.dto.auth.EurecaTokenResponseDTO;
import com.unicar.dto.auth.LoginRequestDTO;
import com.unicar.dto.auth.LoginResponseDTO;
import com.unicar.dto.usuario.UsuarioDTO;
import com.unicar.enums.Genero;
import com.unicar.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class AuthService {
    private static final String EURECA_HEADER_TOKEN = "token-de-autenticacao";

    @Value("${eureca.url.tokens}")
    private String eurecaTokenUrl;

    @Value("${eureca.url.profile}")
    private String eurecaProfileUrl;

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final RestClient restClient;

    public AuthService(UsuarioRepository usuarioRepository, JwtService jwtService, RestClient.Builder restClientBuilder) {
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.restClient = restClientBuilder.build();
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        String eurecaToken = autenticar(request);
        EurecaProfileResponseDTO perfil = consultarPerfilEureca(eurecaToken);
        Usuario usuario = criarOuSincronizarUsuario(perfil);

        log.info("Login Eureca realizado com sucesso para CPF {}", mascararCpf(usuario.getCpf()));

        return new LoginResponseDTO(jwtService.gerarToken(usuario), UsuarioDTO.from(usuario));
    }

    private String autenticar(LoginRequestDTO request) {
        try {
            EurecaTokenResponseDTO response = restClient.post()
                .uri(eurecaTokenUrl)
                .body(Map.of("credentials", Map.of("username", request.usuario(), "password", request.senha())))
                .retrieve()
                .onStatus(
                    status -> status.value() == HttpStatus.UNAUTHORIZED.value(),
                    (req, res) -> {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
                    }
                )
                .onStatus(
                    HttpStatusCode::isError,
                    (req, res) -> {
                        throw new ResponseStatusException(
                            HttpStatus.BAD_GATEWAY,
                            "Erro ao comunicar com o provedor de identidade."
                        );
                    }
                )
                .body(EurecaTokenResponseDTO.class);

            if (response == null || response.token() == null || response.token().isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
            }

            return response.token();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Erro ao autenticar no Eureca", ex);
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Erro ao comunicar com o provedor de autenticação."
            );
        }
    }

    private EurecaProfileResponseDTO consultarPerfilEureca(String eurecaToken) {
        try {
            EurecaProfileResponseDTO perfil = restClient.get()
                .uri(eurecaProfileUrl)
                .header(EURECA_HEADER_TOKEN, eurecaToken)
                .retrieve()
                .onStatus(
                    HttpStatusCode::isError,
                    (req, res) -> {
                        throw new ResponseStatusException(
                            HttpStatus.BAD_GATEWAY,
                            "Erro ao obter perfil institucional."
                        );
                    }
                )
                .body(EurecaProfileResponseDTO.class);

            if (perfil == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Perfil institucional indisponível no Eureca."
                );
            }

            return perfil;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Erro ao consultar perfil no Eureca", ex);
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Erro ao obter perfil institucional."
            );
        }
    }

    private Usuario criarOuSincronizarUsuario(EurecaProfileResponseDTO perfil) {
        Optional<Usuario> existente = Optional.empty();

        if (temTexto(perfil.id())) {
            existente = usuarioRepository.findByCpf(perfil.id());
        }
        if (existente.isEmpty() && temTexto(perfil.matricula())) {
            existente = usuarioRepository.findByMatricula(perfil.matricula());
        }
        if (existente.isEmpty() && temTexto(perfil.email())) {
            existente = usuarioRepository.findByEmail(perfil.email());
        }

        Usuario usuario = existente
            .map(u -> sincronizar(u, perfil))
            .orElseGet(() -> criarUsuario(perfil));

        return usuarioRepository.save(usuario);
    }

    private Usuario criarUsuario(EurecaProfileResponseDTO perfil) {
        return Usuario.builder()
            .cpf(perfil.id())
            .nome(perfil.name())
            .email(perfil.email())
            .matricula(perfil.matricula())
            .curso(perfil.curso())
            .receberEmail(true)
            .ativo(true)
            .genero(Genero.NAO_INFORMADO)
            .build();
    }

    private Usuario sincronizar(Usuario usuario, EurecaProfileResponseDTO perfil) {
        if (temTexto(perfil.id())) {
            usuario.setCpf(perfil.id());
        }
        if (temTexto(perfil.name())) {
            usuario.setNome(perfil.name());
        }
        if (temTexto(perfil.email())) {
            usuario.setEmail(perfil.email());
        }
        if (temTexto(perfil.matricula())) {
            usuario.setMatricula(perfil.matricula());
        }
        if (temTexto(perfil.curso())) {
            usuario.setCurso(perfil.curso());
        }
        usuario.setAtivo(true);
        return usuario;
    }

    private static boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static String mascararCpf(String cpf) {
        if (cpf == null || cpf.length() < 4) {
            return "***";
        }
        return "***" + cpf.substring(cpf.length() - 4);
    }
}