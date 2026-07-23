package com.unicar.quality;

import com.unicar.domain.Usuario;
import com.unicar.dto.auth.EurecaProfileResponseDTO;
import com.unicar.dto.carona.CaronaRequestDTO;
import com.unicar.dto.carona.EnderecoDTO;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.UsuarioService;
import com.unicar.service.auth.JwtService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de regressão para os achados atuais do SpotBugs
 * (./gradlew spotbugsMain/spotbugsTest, relatório em build/reports/spotbugs).
 *
 * Cada teste afirma o comportamento CORRETO (o que deveria acontecer depois da
 * correção) — por isso todos falham hoje, no estado atual do código, e devem
 * passar a partir do momento em que a classe correspondente for corrigida.
 * Nenhuma correção de produção foi feita neste commit.
 *
 * BX_UNBOXING_IMMEDIATELY_REBOXED (AvaliacaoService.buscarReputacao) não tem
 * teste correspondente: é um achado de bytecode (autoboxing/unboxing
 * redundante no ternário `media == null ? 0.0 : media`) sem nenhuma diferença
 * de comportamento observável de fora do método — não existe um "estado
 * correto" observável para afirmar num teste de caixa preta.
 */
class SpotBugsFindingsTest {

    @Nested
    @DisplayName("EI_EXPOSE_REP / EI_EXPOSE_REP2 - EurecaProfileResponseDTO.attributes() deveria fazer cópia defensiva")
    class EurecaProfileResponseDTOExposicaoDeMap {

        @Test
        @DisplayName("Mutar o Map original depois de construir o DTO não deveria alterar attributes()")
        void naoDeveRefletirMutacaoDoMapPassadoNoConstrutor() {
            Map<String, String> atributosOriginais = new HashMap<>();
            atributosOriginais.put("aluno", "2022050");

            EurecaProfileResponseDTO perfil = new EurecaProfileResponseDTO(
                    "id", "nome", "email", "Aluno", atributosOriginais);

            atributosOriginais.put("curso", "Ciência da Computação");

            assertThat(perfil.attributes()).doesNotContainKey("curso");
        }

        @Test
        @DisplayName("Mutar o Map obtido via attributes() não deveria alterar o estado interno do record")
        void naoDeveRefletirMutacaoDoMapObtidoPeloGetter() {
            EurecaProfileResponseDTO perfil = new EurecaProfileResponseDTO(
                    "id", "nome", "email", "Aluno", new HashMap<>());

            try {
                perfil.attributes().put("injetado", "valor");
            } catch (UnsupportedOperationException aceitavelSeImutavel) {
                // Uma cópia imutável (ex.: Map.copyOf) também é uma correção válida.
            }

            assertThat(perfil.attributes()).doesNotContainKey("injetado");
        }
    }

    @Nested
    @DisplayName("EI_EXPOSE_REP / EI_EXPOSE_REP2 - CaronaRequestDTO.datasHorasSaida() deveria fazer cópia defensiva")
    class CaronaRequestDTOExposicaoDeLista {

        private CaronaRequestDTO criarRequest(List<LocalDateTime> datas) {
            return new CaronaRequestDTO(
                    1L,
                    new EnderecoDTO("Origem", new BigDecimal("-7.22"), new BigDecimal("-35.91")),
                    new EnderecoDTO("Destino", new BigDecimal("-7.23"), new BigDecimal("-35.87")),
                    "Ponto de encontro",
                    datas,
                    2,
                    new BigDecimal("5.00"),
                    null);
        }

        @Test
        @DisplayName("Mutar a List original depois de construir o DTO não deveria alterar datasHorasSaida()")
        void naoDeveRefletirMutacaoDaListaPassadaNoConstrutor() {
            List<LocalDateTime> datasOriginais = new ArrayList<>(
                    List.of(LocalDateTime.now().plusDays(1)));
            CaronaRequestDTO request = criarRequest(datasOriginais);

            datasOriginais.add(LocalDateTime.now().plusDays(3));

            assertThat(request.datasHorasSaida()).hasSize(1);
        }

        @Test
        @DisplayName("Mutar a List obtida via datasHorasSaida() não deveria alterar o estado interno do record")
        void naoDeveRefletirMutacaoDaListaObtidaPeloGetter() {
            CaronaRequestDTO request = criarRequest(
                    new ArrayList<>(List.of(LocalDateTime.now().plusDays(1))));

            try {
                request.datasHorasSaida().add(LocalDateTime.now().plusDays(2));
            } catch (UnsupportedOperationException aceitavelSeImutavel) {
                // Uma cópia imutável (ex.: List.copyOf) também é uma correção válida.
            }

            assertThat(request.datasHorasSaida()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("EI_EXPOSE_REP / EI_EXPOSE_REP2 - UsuarioDetails.getUsuario() deveria fazer cópia defensiva")
    class UsuarioDetailsExposicaoDeUsuario {

        @Test
        @DisplayName("Mutar o Usuario obtido via getUsuario() não deveria alterar o Usuario original")
        void naoDeveRefletirMutacaoDoUsuarioObtidoPeloGetter() {
            Usuario original = Usuario.builder().id(1L).nome("Original").build();
            UsuarioDetails usuarioDetails = new UsuarioDetails(original);

            usuarioDetails.getUsuario().setNome("Alterado por fora");

            assertThat(original.getNome()).isEqualTo("Original");
        }
    }

    @Nested
    @DisplayName("SE_BAD_FIELD - UsuarioDetails implementa Serializable (via UserDetails) mas guarda campo não serializável")
    class UsuarioDetailsSerializacao {

        @Test
        @DisplayName("Serializar UsuarioDetails não deveria lançar exceção")
        void deveSerializarSemErro() {
            Usuario usuario = Usuario.builder().id(1L).nome("Teste").build();
            UsuarioDetails usuarioDetails = new UsuarioDetails(usuario);

            assertThatCode(() -> {
                try (ObjectOutputStream out = new ObjectOutputStream(new ByteArrayOutputStream())) {
                    out.writeObject(usuarioDetails);
                }
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("CT_CONSTRUCTOR_THROW - JwtService deveria ser final")
    class JwtServiceDeveriaSerFinal {

        @Test
        @DisplayName("Classe deveria ser declarada final, já que o construtor pode lançar exceção (mitiga ataque de finalizer)")
        void classeDeveriaSerFinal() {
            assertThat(Modifier.isFinal(JwtService.class.getModifiers())).isTrue();
        }
    }

    @Nested
    @DisplayName("UPM_UNCALLED_PRIVATE_METHOD - UsuarioService.buscarUsuarioAtivo(String) deveria ser removido")
    class UsuarioServiceMetodoPrivadoNaoUtilizado {

        @Test
        @DisplayName("O overload buscarUsuarioAtivo(String) não deveria mais existir, já que nenhum método da classe o chama")
        void metodoMortoNaoDeveriaExistir() {
            assertThatThrownBy(() ->
                    UsuarioService.class.getDeclaredMethod("buscarUsuarioAtivo", String.class))
                    .isInstanceOf(NoSuchMethodException.class);
        }
    }
}
