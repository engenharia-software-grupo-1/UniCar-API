package com.unicar.service.bloqueio;

import com.unicar.domain.BloqueioUsuario;
import com.unicar.domain.Usuario;
import com.unicar.dto.bloqueio.BloqueioDTO;
import com.unicar.dto.bloqueio.UsuarioBloqueadoDTO;
import com.unicar.repository.BloqueioUsuarioRepository;
import com.unicar.repository.UsuarioRepository;
import com.unicar.service.BloqueioUsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do Serviço de Bloqueio de Usuários")
class BloqueioUsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private BloqueioUsuarioRepository repository;

    @InjectMocks
    private BloqueioUsuarioService service;

    private Usuario usuarioOrigem;
    private Usuario usuarioAlvo;
    private BloqueioUsuario bloqueioExemplo;

    @BeforeEach
    void setUp() {
        usuarioOrigem = Usuario.builder().id(1L).nome("Jennifer Medeiros").curso("Ciência da Computação").build();
        usuarioAlvo = Usuario.builder().id(5L).nome("João Silva").curso("Engenharia Elétrica")
                .linkFoto("https://cdn.exemplo.com/joao.jpg").build();

        bloqueioExemplo = BloqueioUsuario.builder()
                .id(10L)
                .usuario(usuarioOrigem)
                .usuarioBloqueado(usuarioAlvo)
                .dataBloqueio(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Cenários de Listagem")
    class ListarBloqueados {

        @Test
        @DisplayName("Deve retornar lista de DTOs dos usuários bloqueados com sucesso")
        void deveListarUsuariosBloqueados() {
            when(repository.findAllByUsuarioId(1L)).thenReturn(List.of(bloqueioExemplo));

            List<UsuarioBloqueadoDTO> resultado = service.listarBloqueados(1L);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).id()).isEqualTo(5L);
            assertThat(resultado.get(0).nome()).isEqualTo("João Silva");
            assertThat(resultado.get(0).linkFoto()).isEqualTo("https://cdn.exemplo.com/joao.jpg");
            verify(repository).findAllByUsuarioId(1L);
        }
    }

    @Nested
    @DisplayName("Cenários de Bloqueio")
    class BloquearUsuario {

        @Test
        @DisplayName("Deve lançar erro ao tentar bloquear a si mesmo")
        void deveLancarErroAoBloquearASiMesmo() {
            assertThatThrownBy(() -> service.bloquear(1L, 1L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                    .hasMessageContaining("Não é permitido bloquear a si mesmo");
        }

        @Test
        @DisplayName("Deve lançar erro quando o usuário já estiver bloqueado")
        void deveLancarErroAoBloquearDuplicado() {
            when(repository.existsByUsuarioIdAndUsuarioBloqueadoId(1L, 5L)).thenReturn(true);

            assertThatThrownBy(() -> service.bloquear(1L, 5L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                    .hasMessageContaining("Usuário já bloqueado");
        }

        @Test
        @DisplayName("Deve lançar erro quando o usuário de origem não for encontrado")
        void deveLancarErroSeUsuarioOrigemNaoExiste() {
            when(repository.existsByUsuarioIdAndUsuarioBloqueadoId(1L, 5L)).thenReturn(false);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.bloquear(1L, 5L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                    .hasMessageContaining("Usuário não encontrado");
        }

        @Test
        @DisplayName("Deve executar o bloqueio com sucesso sob condições válidas")
        void deveBloquearComSucesso() {
            when(repository.existsByUsuarioIdAndUsuarioBloqueadoId(1L, 5L)).thenReturn(false);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioOrigem));
            when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuarioAlvo));
            when(repository.save(any(BloqueioUsuario.class))).thenReturn(bloqueioExemplo);

            BloqueioDTO resultado = service.bloquear(1L, 5L);

            assertThat(resultado).isNotNull();
            assertThat(resultado.id()).isEqualTo(10L);
            verify(repository).save(any(BloqueioUsuario.class));
        }
    }

    @Nested
    @DisplayName("Cenários de Desbloqueio")
    class DesbloquearUsuario {

        @Test
        @DisplayName("Deve lançar erro se o registro de bloqueio não for localizado")
        void deveLancarErroSeBloqueioNaoExiste() {
            when(repository.findByUsuarioIdAndUsuarioBloqueadoId(1L, 5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.desbloquear(1L, 5L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                    .hasMessageContaining("Bloqueio não encontrado");
        }

        @Test
        @DisplayName("Deve remover o bloqueio com sucesso")
        void deveDesbloquearComSucesso() {
            when(repository.findByUsuarioIdAndUsuarioBloqueadoId(1L, 5L)).thenReturn(Optional.of(bloqueioExemplo));
            doNothing().when(repository).delete(bloqueioExemplo);

            service.desbloquear(1L, 5L);

            verify(repository).delete(bloqueioExemplo);
        }
    }
}
