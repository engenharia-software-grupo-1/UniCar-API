package com.unicar.service.auth.provider;

import com.unicar.domain.Usuario;
import com.unicar.dto.auth.LoginRequestDTO;
import com.unicar.dto.auth.LoginResponseDTO;
import com.unicar.dto.usuario.UsuarioDTO;
import com.unicar.enums.Genero;
import com.unicar.repository.UsuarioRepository;
import com.unicar.service.auth.JwtService;
import com.unicar.util.PerfilPendenteUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SigaaAuthProvider {

    private static final Pattern LOGIN_FORM_ACTION = Pattern.compile(
        "<form[^>]*name=\"loginForm\"[^>]*action=\"([^\"]+)\"",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern HIDDEN_INPUT = Pattern.compile(
        "<input[^>]*type=\"hidden\"[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern INPUT_NAME = Pattern.compile("name=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern INPUT_VALUE = Pattern.compile("value=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL_USUARIO = Pattern.compile(
        "([a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,})",
        Pattern.CASE_INSENSITIVE
    );

    private static final int CB_THRESHOLD_FAILURES = 3;
    private static final long CB_OPEN_DURATION_MS = 30_000L;

    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicReference<Instant> circuitOpenedAt = new AtomicReference<>(null);

    @Value("${sigaa.url}")
    private String sigaaBaseUrl;

    @Value("${sigaa.login-path}")
    private String loginPath;

    @Value("${unicar.http.connect-timeout-ms:5000}")
    private long connectTimeoutMs;

    @Value("${unicar.http.read-timeout-ms:15000}")
    private long readTimeoutMs;

    @Value("${sigaa.retry.max-attempts:2}")
    private int maxRetryAttempts;

    @Value("${sigaa.retry.backoff-ms:1000}")
    private long retryBackoffMs;

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public LoginResponseDTO autenticar(LoginRequestDTO request) {
        String login = request.usuario().trim();
        String respostaLogin = validarCredenciaisComRetry(login, request.senha());

        Usuario usuario = buscarUsuarioLocal(login)
            .map(u -> { u.setAtivo(true); return u; })
            .orElseGet(() -> criarUsuarioMinimo(login));

        tentarEnriquecerPerfil(usuario, respostaLogin);
        usuario = usuarioRepository.save(usuario);
        log.info("Login SIGAA realizado com sucesso para login '{}'", login);

        return new LoginResponseDTO(jwtService.gerarToken(usuario), UsuarioDTO.from(usuario));
    }

    private String validarCredenciaisComRetry(String login, String senha) {
        verificarCircuito();

        ResponseStatusException ultimoErro = null;

        for (int tentativa = 1; tentativa <= maxRetryAttempts; tentativa++) {
            try {
                String resultado = validarCredenciais(login, senha);
                consecutiveFailures.set(0);
                circuitOpenedAt.set(null);
                return resultado;
            } catch (ResponseStatusException ex) {
                if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    throw ex;
                }
                ultimoErro = ex;
                log.warn("Tentativa {}/{} falhou ao autenticar no SIGAA: {}",
                    tentativa, maxRetryAttempts, ex.getReason());
                if (tentativa < maxRetryAttempts) {
                    aguardarBackoff(tentativa);
                }
            }
        }

        int falhas = consecutiveFailures.incrementAndGet();
        if (falhas >= CB_THRESHOLD_FAILURES) {
            circuitOpenedAt.compareAndSet(null, Instant.now());
            log.warn("Circuit breaker aberto após {} falhas consecutivas no SIGAA.", falhas);
        }

        throw ultimoErro != null ? ultimoErro
            : new ResponseStatusException(HttpStatus.BAD_GATEWAY, "SIGAA indisponível.");
    }

    private void verificarCircuito() {
        Instant aberturaEm = circuitOpenedAt.get();
        if (aberturaEm == null) {
            return;
        }

        long decorrido = Instant.now().toEpochMilli() - aberturaEm.toEpochMilli();
        if (decorrido < CB_OPEN_DURATION_MS) {
            long restante = (CB_OPEN_DURATION_MS - decorrido) / 1000;
            log.warn("Circuit breaker ABERTO. Tentando novamente em ~{}s.", restante);
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Serviço de autenticação temporariamente indisponível. Tente novamente em instantes."
            );
        }
        log.info("Circuit breaker em half-open: testando SIGAA...");
    }

    private void aguardarBackoff(int tentativa) {
        long espera = retryBackoffMs * (long) Math.pow(2, tentativa - 1);
        try {
            log.debug("Aguardando {}ms antes da próxima tentativa...", espera);
            Thread.sleep(espera);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Autenticação interrompida.");
        }
    }

    private String validarCredenciais(String login, String senha) {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient httpClient = HttpClient.newBuilder()
            .cookieHandler(cookieManager)
            .connectTimeout(Duration.ofMillis(connectTimeoutMs))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .version(HttpClient.Version.HTTP_1_1)
            .build();

        try {
            HttpResponse<String> loginPage = httpClient.send(
                HttpRequest.newBuilder()
                    .uri(URI.create(resolverUrl(loginPath)))
                    .header("User-Agent", "UniCar-API/1.0")
                    .GET()
                    .timeout(Duration.ofMillis(readTimeoutMs))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );

            if (loginPage.statusCode() >= 500) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "SIGAA indisponível.");
            }

            String htmlLogin = loginPage.body();
            String action = extrairAcaoLogin(htmlLogin);
            Map<String, String> formData = montarFormularioLogin(login, senha, htmlLogin);

            HttpResponse<String> resposta = httpClient.send(
                HttpRequest.newBuilder()
                    .uri(URI.create(resolverUrl(action)))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", "UniCar-API/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(codificarFormulario(formData)))
                    .timeout(Duration.ofMillis(readTimeoutMs))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );

            if (resposta.statusCode() >= 500) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "SIGAA indisponível.");
            }

            String htmlResposta = resposta.body();
            if (credenciaisInvalidas(htmlResposta)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
            }

            return htmlResposta;

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Erro de I/O ao autenticar no SIGAA: {}", ex.getMessage());
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Erro ao comunicar com o provedor de autenticação alternativo."
            );
        }
    }

    private Optional<Usuario> buscarUsuarioLocal(String login) {
        return usuarioRepository.findByMatricula(login)
            .or(() -> usuarioRepository.findByEmail(login))
            .or(() -> login.matches("\\d{11}")
                ? usuarioRepository.findByCpf(login)
                : Optional.empty());
    }

    private Usuario criarUsuarioMinimo(String login) {
        return Usuario.builder()
            .matricula(PerfilPendenteUtil.matriculaPendente(login))
            .nome(PerfilPendenteUtil.nomePendente(login))
            .email(PerfilPendenteUtil.emailPendente(login))
            .cpf(PerfilPendenteUtil.cpfParaCadastro(login))
            .receberEmail(true)
            .ativo(true)
            .genero(Genero.NAO_INFORMADO)
            .build();
    }

    private void tentarEnriquecerPerfil(Usuario usuario, String html) {
        try {
            extrairEmail(html).ifPresent(email -> {
                if (PerfilPendenteUtil.isEmailPendente(usuario.getEmail())) {
                    usuario.setEmail(email.trim().toLowerCase());
                }
            });
        } catch (RuntimeException ex) {
            log.debug("Enriquecimento opcional de perfil via SIGAA ignorado: {}", ex.getMessage());
        }
    }

    private Map<String, String> montarFormularioLogin(String login, String senha, String htmlLogin) {
        Map<String, String> formData = new LinkedHashMap<>();
        extrairCamposOcultos(htmlLogin).forEach(formData::putIfAbsent);
        formData.putIfAbsent("width", "0");
        formData.putIfAbsent("height", "0");
        formData.putIfAbsent("urlRedirect", "");
        formData.putIfAbsent("subsistemaRedirect", "");
        formData.putIfAbsent("acao", "");
        formData.putIfAbsent("acessibilidade", "");
        formData.put("user.login", login);
        formData.put("user.senha", senha);
        return formData;
    }

    private Map<String, String> extrairCamposOcultos(String html) {
        Map<String, String> campos = new LinkedHashMap<>();
        Matcher matcher = HIDDEN_INPUT.matcher(html);
        while (matcher.find()) {
            String input = matcher.group();
            Matcher nameMatcher = INPUT_NAME.matcher(input);
            Matcher valMatcher = INPUT_VALUE.matcher(input);
            if (nameMatcher.find()) {
                campos.put(nameMatcher.group(1), valMatcher.find() ? valMatcher.group(1) : "");
            }
        }
        return campos;
    }

    private String extrairAcaoLogin(String html) {
        Matcher matcher = LOGIN_FORM_ACTION.matcher(html);
        if (!matcher.find()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Formulário de login do SIGAA indisponível.");
        }
        return matcher.group(1);
    }

    private boolean credenciaisInvalidas(String html) {
        String lower = html.toLowerCase();
        if (lower.contains("usu") && lower.contains("senha inv")) {
            return true;
        }
        if (lower.contains("login ou senha incorretos")) {
            return true;
        }
        if (lower.contains("acesso negado")) {
            return true;
        }
        return html.contains("name=\"loginForm\"") && html.contains("name=\"user.senha\"");
    }

    private Optional<String> extrairEmail(String html) {
        Matcher matcher = EMAIL_USUARIO.matcher(html);
        while (matcher.find()) {
            String email = matcher.group(1);
            if (!email.endsWith("@pendente.unicar") && !email.contains("example.com")) {
                return Optional.of(email);
            }
        }
        return Optional.empty();
    }

    private String resolverUrl(String path) {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String base = sigaaBaseUrl.endsWith("/")
            ? sigaaBaseUrl.substring(0, sigaaBaseUrl.length() - 1)
            : sigaaBaseUrl;
        return path.startsWith("/") ? base + path : base + "/" + path;
    }

    private static String codificarFormulario(Map<String, String> formData) {
        return formData.entrySet().stream()
            .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
    }
}
