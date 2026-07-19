package com.unicar.service.auth;

import com.unicar.dto.auth.LoginRequestDTO;
import com.unicar.dto.auth.LoginResponseDTO;
import com.unicar.enums.Genero;
import com.unicar.domain.Usuario;
import com.unicar.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
 
    private static final String TOKEN_URL = "http://eureca.test/tokens";
    private static final String PROFILE_URL = "http://eureca.test/profile";
    private static final String ESTUDANTE_URL = "http://eureca.test/estudante";
    private static final String PROFESSOR_URL = "http://eureca.test/professor";
    private static final String EURECA_HEADER_TOKEN = "token-de-autenticacao";
 
    @Mock
    private UsuarioRepository usuarioRepository;
 
    @Mock
    private JwtService jwtService;
 
    @Mock
    private BlacklistService blacklistService;
 
    private MockRestServiceServer mockServer;
    private AuthService authService;
 
    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
 
        authService = new AuthService(usuarioRepository, jwtService, builder, blacklistService);
 
        ReflectionTestUtils.setField(authService, "eurecaTokenUrl", TOKEN_URL);
        ReflectionTestUtils.setField(authService, "eurecaProfileUrl", PROFILE_URL);
        ReflectionTestUtils.setField(authService, "eurecaEstudanteUrl", ESTUDANTE_URL);
        ReflectionTestUtils.setField(authService, "eurecaProfessorUrl", PROFESSOR_URL);
    }
 
    private LoginRequestDTO requestValido() {
        return new LoginRequestDTO("joao123", "senha-secreta");
    }
 
    @Nested
    class Login {
 
        @Test
        void deveAutenticarAlunoComSucessoQuandoUsuarioNaoExiste() {
            mockServer.expect(requestTo(TOKEN_URL))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("""
                    {"token": "eureca-token-abc"}
                    """, MediaType.APPLICATION_JSON));
 
            mockServer.expect(requestTo(PROFILE_URL))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header(EURECA_HEADER_TOKEN, "eureca-token-abc"))
                .andRespond(withSuccess("""
                    {
                      "id": "12345678900",
                      "name": "Joao da Silva",
                      "email": "joao@unicar.edu.br",
                      "type": "Aluno",
                      "attributes": {"aluno": "2021001"}
                    }
                    """, MediaType.APPLICATION_JSON));
 
            mockServer.expect(requestToUriTemplate(ESTUDANTE_URL + "?estudante={matricula}", "2021001"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("""
                    {
                      "matricula_do_estudante": "2021001",
                      "nome_do_curso": "Ciência da Computação",
                      "sexo": "M",
                      "cpf": "12345678900"
                    }
                    """, MediaType.APPLICATION_JSON));
 
            given(usuarioRepository.findByCpf("12345678900")).willReturn(Optional.empty());
            given(usuarioRepository.findByMatricula("2021001")).willReturn(Optional.empty());
            given(usuarioRepository.findByEmail("joao@unicar.edu.br")).willReturn(Optional.empty());
            given(usuarioRepository.save(any(Usuario.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
            given(jwtService.gerarToken(any(Usuario.class))).willReturn("jwt-gerado");
 
            LoginResponseDTO response = authService.login(requestValido());
 
            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo("jwt-gerado");
 
            mockServer.verify();
            verify(usuarioRepository).save(any(Usuario.class));
        }
 
        @Test
        void deveCairParaFluxoDeDocenteQuandoNaoForAluno() {
            mockServer.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("""
                    {"token": "eureca-token-prof"}
                    """, MediaType.APPLICATION_JSON));
 
            mockServer.expect(requestTo(PROFILE_URL))
                .andExpect(header(EURECA_HEADER_TOKEN, "eureca-token-prof"))
                .andRespond(withSuccess("""
                    {
                      "id": "98765432100",
                      "name": "Maria Professora",
                      "email": "maria@unicar.edu.br",
                      "type": "Docente",
                      "attributes": {"siape": "555111"}
                    }
                    """, MediaType.APPLICATION_JSON));
 
            mockServer.expect(requestToUriTemplate(PROFESSOR_URL + "?professor={siape}", "555111"))
                .andRespond(withSuccess("""
                    [{"matriculaDoDocente": 555111, "cpf": "98765432100"}]
                    """, MediaType.APPLICATION_JSON));
 
            given(usuarioRepository.findByCpf("98765432100")).willReturn(Optional.empty());
            given(usuarioRepository.findByMatricula("555111")).willReturn(Optional.empty());
            given(usuarioRepository.findByEmail("maria@unicar.edu.br")).willReturn(Optional.empty());
            given(usuarioRepository.save(any(Usuario.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
            given(jwtService.gerarToken(any(Usuario.class))).willReturn("jwt-docente");
 
            LoginResponseDTO response = authService.login(requestValido());
 
            assertThat(response.token()).isEqualTo("jwt-docente");
            mockServer.verify();
        }
 
        @Test
        void deveSincronizarUsuarioExistenteMantendoDadosQuandoJaCadastrado() {
            mockServer.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("""
                    {"token": "eureca-token-xyz"}
                    """, MediaType.APPLICATION_JSON));
 
            mockServer.expect(requestTo(PROFILE_URL))
                .andRespond(withSuccess("""
                    {
                      "id": "11122233344",
                      "name": "Carlos Aluno",
                      "email": "carlos@unicar.edu.br",
                      "type": "Aluno",
                      "attributes": {"aluno": "2020099"}
                    }
                    """, MediaType.APPLICATION_JSON));
 
            mockServer.expect(requestToUriTemplate(ESTUDANTE_URL + "?estudante={matricula}", "2020099"))
                .andRespond(withSuccess("""
                    {
                      "matricula_do_estudante": "2020099",
                      "nome_do_curso": "Engenharia de Software",
                      "sexo": "M",
                      "cpf": "11122233344"
                    }
                    """, MediaType.APPLICATION_JSON));
 
            Usuario existente = Usuario.builder()
                .cpf("11122233344")
                .nome("Carlos Antigo Nome")
                .email("carlos.antigo@unicar.edu.br")
                .matricula("2020099")
                .curso("Curso Antigo")
                .genero(Genero.NAO_INFORMADO)
                .ativo(false)
                .build();
 
            given(usuarioRepository.findByCpf("11122233344")).willReturn(Optional.of(existente));
            given(usuarioRepository.save(any(Usuario.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
            given(jwtService.gerarToken(any(Usuario.class))).willReturn("jwt-sync");
 
            authService.login(requestValido());
 
            assertThat(existente.getNome()).isEqualTo("Carlos Aluno");
            assertThat(existente.getEmail()).isEqualTo("carlos@unicar.edu.br");
            assertThat(existente.getCurso()).isEqualTo("Engenharia de Software");
            assertThat(existente.getAtivo()).isTrue();
 
            verify(usuarioRepository, never()).findByMatricula(anyString());
        }
 
        @Test
        void deveLancarUnauthorizedQuandoCredenciaisInvalidas() {
            mockServer.expect(requestTo(TOKEN_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
 
            assertThatThrownBy(() -> authService.login(requestValido()))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.UNAUTHORIZED);
 
            mockServer.verify();
            verify(usuarioRepository, never()).save(any());
        }
 
        @Test
        void deveLancarBadGatewayQuandoEurecaTokenEndpointFalha() {
            mockServer.expect(requestTo(TOKEN_URL))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
 
            assertThatThrownBy(() -> authService.login(requestValido()))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_GATEWAY);
        }
 
        @Test
        void deveLancarUnauthorizedQuandoTokenRetornadoVazio() {
            mockServer.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("""
                    {"token": ""}
                    """, MediaType.APPLICATION_JSON));
 
            assertThatThrownBy(() -> authService.login(requestValido()))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.UNAUTHORIZED);
        }
 
        @Test
        void deveLancarBadGatewayQuandoPerfilFalha() {
            mockServer.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("""
                    {"token": "token-ok"}
                    """, MediaType.APPLICATION_JSON));
 
            mockServer.expect(requestTo(PROFILE_URL))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
 
            assertThatThrownBy(() -> authService.login(requestValido()))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_GATEWAY);
        }
 
        @Test
        void deveSanitizarCpfFormatadoAntesDeGravar() {
            mockServer.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("""
                    {"token": "eureca-token-fmt"}
                    """, MediaType.APPLICATION_JSON));

            mockServer.expect(requestTo(PROFILE_URL))
                .andRespond(withSuccess("""
                    {
                      "id": "clara.estudante",
                      "name": "Clara Estudante",
                      "email": "clara@unicar.edu.br",
                      "type": "Aluno",
                      "attributes": {"aluno": "2022050"}
                    }
                    """, MediaType.APPLICATION_JSON));

            mockServer.expect(requestToUriTemplate(ESTUDANTE_URL + "?estudante={matricula}", "2022050"))
                .andRespond(withSuccess("""
                    {
                      "matricula_do_estudante": "2022050",
                      "nome_do_curso": "Ciência da Computação",
                      "sexo": "F",
                      "cpf": "111.222.333-44"
                    }
                    """, MediaType.APPLICATION_JSON));

            given(usuarioRepository.findByCpf("11122233344")).willReturn(Optional.empty());
            given(usuarioRepository.findByMatricula("2022050")).willReturn(Optional.empty());
            given(usuarioRepository.findByEmail("clara@unicar.edu.br")).willReturn(Optional.empty());
            given(usuarioRepository.save(any(Usuario.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
            given(jwtService.gerarToken(any(Usuario.class))).willReturn("jwt-clara");

            authService.login(requestValido());

            org.mockito.ArgumentCaptor<Usuario> captor = org.mockito.ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            assertThat(captor.getValue().getCpf()).isEqualTo("11122233344");
        }

        @Test
        void deveRecomporCpfComZeroALEsquerdaPerdidoPeloEureca() {
            mockServer.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("""
                    {"token": "eureca-token-zero"}
                    """, MediaType.APPLICATION_JSON));

            mockServer.expect(requestTo(PROFILE_URL))
                .andRespond(withSuccess("""
                    {
                      "id": "usuario.comzero",
                      "name": "Usuario Com Zero",
                      "email": "comzero@unicar.edu.br",
                      "type": "Aluno",
                      "attributes": {"aluno": "2022099"}
                    }
                    """, MediaType.APPLICATION_JSON));

            mockServer.expect(requestToUriTemplate(ESTUDANTE_URL + "?estudante={matricula}", "2022099"))
                .andRespond(withSuccess("""
                    {
                      "matricula_do_estudante": "2022099",
                      "nome_do_curso": "Ciência da Computação",
                      "sexo": "M",
                      "cpf": "1122233396"
                    }
                    """, MediaType.APPLICATION_JSON));

            given(usuarioRepository.findByCpf("01122233396")).willReturn(Optional.empty());
            given(usuarioRepository.findByMatricula("2022099")).willReturn(Optional.empty());
            given(usuarioRepository.findByEmail("comzero@unicar.edu.br")).willReturn(Optional.empty());
            given(usuarioRepository.save(any(Usuario.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
            given(jwtService.gerarToken(any(Usuario.class))).willReturn("jwt-comzero");

            authService.login(requestValido());

            org.mockito.ArgumentCaptor<Usuario> captor = org.mockito.ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            assertThat(captor.getValue().getCpf()).isEqualTo("01122233396");
        }

        @ParameterizedTest(name = "cpfEureca={0} -> cpfEsperado={1}, badGateway={2}")
        @CsvSource({
                "123456789,x,true",
                "1234567890,01234567890,false",
                "12345678901,12345678901,false",
                "123456789012,x,true"
        })
        @DisplayName("Deve validar as fronteiras de sanitização do CPF por quantidade de dígitos")
        void deveValidarFronteirasDeSanitizacaoDoCpf(String cpfBruto, String cpfEsperado, boolean deveLancarBadGateway) {
            mockServer.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("""
                    {"token": "eureca-token-particao"}
                    """, MediaType.APPLICATION_JSON));

            mockServer.expect(requestTo(PROFILE_URL))
                .andRespond(withSuccess("""
                    {
                      "id": "usuario.particao",
                      "name": "Usuario Particao",
                      "email": "particao@unicar.edu.br",
                      "type": "Aluno",
                      "attributes": {"aluno": "2025001"}
                    }
                    """, MediaType.APPLICATION_JSON));

            mockServer.expect(requestToUriTemplate(ESTUDANTE_URL + "?estudante={matricula}", "2025001"))
                .andRespond(withSuccess("""
                    {
                      "matricula_do_estudante": "2025001",
                      "nome_do_curso": "Ciência da Computação",
                      "sexo": "F",
                      "cpf": "__CPF__"
                    }
                    """.replace("__CPF__", cpfBruto), MediaType.APPLICATION_JSON));

            if (deveLancarBadGateway) {
                assertThatThrownBy(() -> authService.login(requestValido()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_GATEWAY);

                verify(usuarioRepository, never()).save(any());
                return;
            }

            given(usuarioRepository.findByCpf(cpfEsperado)).willReturn(Optional.empty());
            given(usuarioRepository.findByMatricula("2025001")).willReturn(Optional.empty());
            given(usuarioRepository.findByEmail("particao@unicar.edu.br")).willReturn(Optional.empty());
            given(usuarioRepository.save(any(Usuario.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
            given(jwtService.gerarToken(any(Usuario.class))).willReturn("jwt-particao");

            authService.login(requestValido());

            org.mockito.ArgumentCaptor<Usuario> captor = org.mockito.ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            assertThat(captor.getValue().getCpf()).isEqualTo(cpfEsperado);
        }

        @Test
        void deveAutenticarComSucessoQuandoIdDoPerfilForNomeDoUsuario() {
            mockServer.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("""
                    {"token": "eureca-token-nome"}
                    """, MediaType.APPLICATION_JSON));

            mockServer.expect(requestTo(PROFILE_URL))
                .andRespond(withSuccess("""
                    {
                      "id": "Clara Estudante Completo",
                      "name": "Clara Estudante Completo",
                      "email": "clara.nome@unicar.edu.br",
                      "type": "Aluno",
                      "attributes": {"aluno": "2024001"}
                    }
                    """, MediaType.APPLICATION_JSON));

            mockServer.expect(requestToUriTemplate(ESTUDANTE_URL + "?estudante={matricula}", "2024001"))
                .andRespond(withSuccess("""
                    {
                      "matricula_do_estudante": "2024001",
                      "nome_do_curso": "Ciência da Computação",
                      "sexo": "F",
                      "cpf": "22233344455"
                    }
                    """, MediaType.APPLICATION_JSON));

            given(usuarioRepository.findByCpf("22233344455")).willReturn(Optional.empty());
            given(usuarioRepository.findByMatricula("2024001")).willReturn(Optional.empty());
            given(usuarioRepository.findByEmail("clara.nome@unicar.edu.br")).willReturn(Optional.empty());
            given(usuarioRepository.save(any(Usuario.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
            given(jwtService.gerarToken(any(Usuario.class))).willReturn("jwt-nome");

            LoginResponseDTO response = authService.login(requestValido());

            assertThat(response.token()).isEqualTo("jwt-nome");

            org.mockito.ArgumentCaptor<Usuario> captor = org.mockito.ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            assertThat(captor.getValue().getCpf()).isEqualTo("22233344455");
        }

        @Test
        void deveAutenticarComSucessoQuandoIdDoPerfilForEmailDoUsuario() {
            mockServer.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("""
                    {"token": "eureca-token-email"}
                    """, MediaType.APPLICATION_JSON));

            mockServer.expect(requestTo(PROFILE_URL))
                .andRespond(withSuccess("""
                    {
                      "id": "clara.email@unicar.edu.br",
                      "name": "Clara Estudante Email",
                      "email": "clara.email@unicar.edu.br",
                      "type": "Aluno",
                      "attributes": {"aluno": "2024002"}
                    }
                    """, MediaType.APPLICATION_JSON));

            mockServer.expect(requestToUriTemplate(ESTUDANTE_URL + "?estudante={matricula}", "2024002"))
                .andRespond(withSuccess("""
                    {
                      "matricula_do_estudante": "2024002",
                      "nome_do_curso": "Ciência da Computação",
                      "sexo": "F",
                      "cpf": "33344455566"
                    }
                    """, MediaType.APPLICATION_JSON));

            given(usuarioRepository.findByCpf("33344455566")).willReturn(Optional.empty());
            given(usuarioRepository.findByMatricula("2024002")).willReturn(Optional.empty());
            given(usuarioRepository.findByEmail("clara.email@unicar.edu.br")).willReturn(Optional.empty());
            given(usuarioRepository.save(any(Usuario.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
            given(jwtService.gerarToken(any(Usuario.class))).willReturn("jwt-email");

            LoginResponseDTO response = authService.login(requestValido());

            assertThat(response.token()).isEqualTo("jwt-email");

            org.mockito.ArgumentCaptor<Usuario> captor = org.mockito.ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            assertThat(captor.getValue().getCpf()).isEqualTo("33344455566");
        }

        @Test
        void deveLancarBadGatewayQuandoNaoHaAtributosDeAlunoNemDocente() {
            mockServer.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("""
                    {"token": "token-ok"}
                    """, MediaType.APPLICATION_JSON));
 
            mockServer.expect(requestTo(PROFILE_URL))
                .andRespond(withSuccess("""
                    {
                      "id": "00011122233",
                      "name": "Sem Vinculo",
                      "email": "semvinculo@unicar.edu.br",
                      "type": "Outro",
                      "attributes": {}
                    }
                    """, MediaType.APPLICATION_JSON));
 
            assertThatThrownBy(() -> authService.login(requestValido()))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_GATEWAY);
 
            verify(usuarioRepository, never()).save(any());
        }
    }
 
    @Nested
    class Logout {
 
        @Test
        void deveRevogarTokenQuandoValido() {
            String token = "token-valido";
            Instant expiracao = Instant.now().plusSeconds(3600);
 
            given(jwtService.tokenValido(token)).willReturn(true);
            given(jwtService.extrairExpiracao(token)).willReturn(expiracao);
 
            authService.logout(token);
 
            verify(blacklistService, times(1)).revogar(token, expiracao);
        }
 
        @Test
        void deveLancarUnauthorizedQuandoTokenInvalido() {
            String token = "token-invalido";
            given(jwtService.tokenValido(token)).willReturn(false);
 
            assertThatThrownBy(() -> authService.logout(token))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.UNAUTHORIZED);
 
            verify(blacklistService, never()).revogar(anyString(), any());
        }
    }
}
