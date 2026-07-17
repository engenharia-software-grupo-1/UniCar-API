package com.unicar.service.auth;

import com.unicar.domain.Usuario;
import com.unicar.dto.auth.EurecaEstudanteResponseDTO;
import com.unicar.dto.auth.EurecaProfessorResponseDTO;
import com.unicar.dto.auth.EurecaProfileResponseDTO;
import com.unicar.dto.auth.EurecaTokenResponseDTO;
import com.unicar.dto.auth.LoginRequestDTO;
import com.unicar.dto.auth.LoginResponseDTO;
import com.unicar.dto.usuario.UsuarioDTO;
import com.unicar.enums.Genero;
import com.unicar.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.List;
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

    @Value("${eureca.url.estudante}")
    private String eurecaEstudanteUrl;

    @Value("${eureca.url.professor}")
    private String eurecaProfessorUrl;

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final RestClient restClient;
    private final BlacklistService blacklistService;

    public AuthService(UsuarioRepository usuarioRepository, JwtService jwtService, RestClient.Builder restClientBuilder, BlacklistService blacklistService) {
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.restClient = restClientBuilder.build();
        this.blacklistService = blacklistService;
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        String eurecaToken = autenticar(request);
        EurecaProfileResponseDTO perfil = consultarPerfilEureca(eurecaToken);
        Usuario usuario = criarOuSincronizarUsuario(eurecaToken, perfil);

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

    public void logout(String accessToken) {
        if (!jwtService.tokenValido(accessToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido ou expirado.");
        }

        Instant expiracao = jwtService.extrairExpiracao(accessToken);
        blacklistService.revogar(accessToken, expiracao);

        log.info("Logout realizado.");
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

    private record DadosAdicionais(String cpf, String matricula, String curso, Genero genero) {}

    private Usuario criarOuSincronizarUsuario(String eurecaToken, EurecaProfileResponseDTO perfil) {
        DadosAdicionais dados = obterDadosAdicionais(eurecaToken, perfil);
        Optional<Usuario> existente = buscarUsuarioExistente(perfil, dados);
    
        Usuario usuario = existente
            .map(u -> sincronizar(u, perfil, dados))
            .orElseGet(() -> criarUsuario(perfil, dados));

        return usuarioRepository.save(usuario);
    }

    private Optional<Usuario> buscarUsuarioExistente(EurecaProfileResponseDTO perfil, DadosAdicionais dados) {
        Optional<Usuario> existente = Optional.empty();

        String cpf = sanitizarCpf(dados.cpf());
        if (temTexto(cpf)) {
            existente = usuarioRepository.findByCpf(cpf);
        }
        if (existente.isEmpty() && temTexto(dados.matricula())) {
            existente = usuarioRepository.findByMatricula(dados.matricula());
        }
        if (existente.isEmpty() && temTexto(perfil.email())) {
            existente = usuarioRepository.findByEmail(perfil.email());
        }

        return existente;
    }

    private DadosAdicionais obterDadosAdicionais(String eurecaToken, EurecaProfileResponseDTO perfil) {
        DadosAdicionais dadosAluno = tentarObterDadosDeAluno(eurecaToken, perfil);
        if (dadosAluno != null) {
            return dadosAluno;
        }

        DadosAdicionais dadosDocente = tentarObterDadosDeDocente(eurecaToken, perfil);
        if (dadosDocente != null) {
            return dadosDocente;
        }

        log.warn("Não foi possível obter matrícula de estudante ou docente no Eureca para o e-mail {}", perfil.email());
        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY,
            "Não foi possível obter os dados acadêmicos do usuário no Eureca."
        );
    }

    private DadosAdicionais tentarObterDadosDeAluno(String eurecaToken, EurecaProfileResponseDTO perfil) {
        if (!"Aluno".equalsIgnoreCase(perfil.type())) {
            return null;
        }

        String matricula = extrairAtributo(perfil, "aluno");
        if (!temTexto(matricula)) {
            return null;
        }

        try {
            EurecaEstudanteResponseDTO estudante = consultarEstudante(eurecaToken, matricula);
            if (estudante == null) {
                return null;
            }

            String matriculaResolvida = temTexto(estudante.matriculaDoEstudante())
                ? estudante.matriculaDoEstudante()
                : matricula;

            return new DadosAdicionais(estudante.cpf(), matriculaResolvida, estudante.nomeDoCurso(), mapearGenero(estudante.sexo()));
        } catch (Exception ex) {
            log.warn("Falha ao consultar dados de estudante no Eureca (matrícula {}), tentando fallback de docente", matricula, ex);
            return null;
        }
    }

    private DadosAdicionais tentarObterDadosDeDocente(String eurecaToken, EurecaProfileResponseDTO perfil) {
        String siape = extrairAtributo(perfil, null);
        if (!temTexto(siape)) {
            return null;
        }

        try {
            EurecaProfessorResponseDTO professor = consultarProfessor(eurecaToken, siape);
            if (professor == null) {
                return null;
            }

            String matricula = professor.matriculaDoDocente() != null
                ? String.valueOf(professor.matriculaDoDocente())
                : siape;

            return new DadosAdicionais(professor.cpf(), matricula, null, Genero.NAO_INFORMADO);
        } catch (Exception ex) {
            log.warn("Falha ao consultar dados de docente no Eureca (siape {})", siape, ex);
            return null;
        }
    }

    private EurecaEstudanteResponseDTO consultarEstudante(String eurecaToken, String matricula) {
        try {
            String uri = UriComponentsBuilder.fromUriString(eurecaEstudanteUrl)
                .queryParam("estudante", matricula)
                .toUriString();

            return restClient.get()
                .uri(uri)
                .header(EURECA_HEADER_TOKEN, eurecaToken)
                .retrieve()
                .onStatus(
                    HttpStatusCode::isError,
                    (req, res) -> {
                        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Erro ao consultar dados de estudante no Eureca.");
                    }
                )
                .body(EurecaEstudanteResponseDTO.class);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Erro ao consultar dados de estudante no Eureca.", ex);
        }
    }

    private EurecaProfessorResponseDTO consultarProfessor(String eurecaToken, String siape) {
        try {
            String uri = UriComponentsBuilder.fromUriString(eurecaProfessorUrl)
                .queryParam("professor", siape)
                .toUriString();

            List<EurecaProfessorResponseDTO> professores = restClient.get()
                .uri(uri)
                .header(EURECA_HEADER_TOKEN, eurecaToken)
                .retrieve()
                .onStatus(
                    HttpStatusCode::isError,
                    (req, res) -> {
                        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Erro ao consultar dados de docente no Eureca.");
                    }
                )
                .body(new ParameterizedTypeReference<List<EurecaProfessorResponseDTO>>() {});

            if (professores == null || professores.isEmpty()) {
                return null;
            }
            return professores.get(0);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Erro ao consultar dados de docente no Eureca.", ex);
        }
    }

    private static String extrairAtributo(EurecaProfileResponseDTO perfil, String chavePreferida) {
        if (perfil.attributes() == null || perfil.attributes().isEmpty()) {
            return null;
        }
        if (chavePreferida != null && temTexto(perfil.attributes().get(chavePreferida))) {
            return perfil.attributes().get(chavePreferida);
        }
        return perfil.attributes().values().stream()
            .filter(AuthService::temTexto)
            .findFirst()
            .orElse(null);
    }

    private static Genero mapearGenero(String sexo) {
        if (!temTexto(sexo)) {
            return Genero.NAO_INFORMADO;
        }
        return switch (sexo.trim().toUpperCase()) {
            case "F" -> Genero.FEMININO;
            case "M" -> Genero.MASCULINO;
            default -> Genero.NAO_INFORMADO;
        };
    }

    // ---- Criação / sincronização do usuário ----

    private Usuario criarUsuario(EurecaProfileResponseDTO perfil, DadosAdicionais dados) {
        return Usuario.builder()
            .cpf(validarCpf(dados.cpf()))
            .nome(perfil.name())
            .email(perfil.email())
            .matricula(dados.matricula())
            .curso(dados.curso())
            .receberEmail(true)
            .ativo(true)
            .genero(dados.genero())
            .build();
    }

    private Usuario sincronizar(Usuario usuario, EurecaProfileResponseDTO perfil, DadosAdicionais dados) {
        if (temTexto(dados.cpf())) {
            usuario.setCpf(validarCpf(dados.cpf()));
        }
        if (temTexto(perfil.name())) {
            usuario.setNome(perfil.name());
        }
        if (temTexto(perfil.email())) {
            usuario.setEmail(perfil.email());
        }
        if (temTexto(dados.matricula())) {
            usuario.setMatricula(dados.matricula());
        }
        if (temTexto(dados.curso())) {
            usuario.setCurso(dados.curso());
        }
        if (dados.genero() != null && dados.genero() != Genero.NAO_INFORMADO) {
            usuario.setGenero(dados.genero());
        }
        usuario.setAtivo(true);
        return usuario;
    }

    private static boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static final int TAMANHO_CPF = 11;

    private static String sanitizarCpf(String cpf) {
        if (cpf == null) {
            return null;
        }
        String digitos = cpf.replaceAll("\\D", "");
        // Alguns retornos do Eureca tratam o CPF como número em algum ponto do
        // pipeline, o que descarta o zero à esquerda (ex: 08539337401 vira
        // 8539337401). Como o CPF sempre tem 11 dígitos, é seguro recompor.
        if (digitos.length() == TAMANHO_CPF - 1) {
            digitos = "0" + digitos;
        }
        return digitos;
    }

    private static String validarCpf(String cpfBruto) {
        String cpf = sanitizarCpf(cpfBruto);
        if (cpf == null || cpf.length() != TAMANHO_CPF) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "O CPF retornado pelo Eureca é inválido."
            );
        }
        return cpf;
    }

    private static String mascararCpf(String cpf) {
        if (cpf == null || cpf.length() < 4) {
            return "***";
        }
        return "***" + cpf.substring(cpf.length() - 4);
    }
}