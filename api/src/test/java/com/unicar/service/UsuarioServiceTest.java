package com.unicar.service;

import com.unicar.domain.Usuario;
import com.unicar.dto.avaliacao.ReputacaoDTO;
import com.unicar.dto.usuario.PerfilUsuarioDTO;
import com.unicar.dto.usuario.UpdatePerfilRequestDTO;
import com.unicar.dto.usuario.UsuarioDTO;
import com.unicar.dto.usuario.UsuarioPublicoDTO;
import com.unicar.enums.Genero;
import com.unicar.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    @Mock
    private AvaliacaoService avaliacaoService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .matricula("20230001")
                .nome("Oscar Rodrigues")
                .email("oscar@teste.com")
                .cpf("12345678901")
                .curso("Computação")
                .ativo(true)
                .receberEmail(true)
                .genero(Genero.NAO_INFORMADO)
                .dataCriacao(LocalDateTime.now())
                .dataAtualizacao(LocalDateTime.now())
                .build();
    }

    private void mockSaveRetornandoMesmoUsuario() {
        when(usuarioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("buscarPerfil")
    class BuscarPerfil {

        @Test
        @DisplayName("deve retornar o perfil quando o usuário existe e está ativo")
        void deveBuscarPerfil() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

            UsuarioDTO dto = usuarioService.buscarPerfil(1L);

            assertThat(dto).isNotNull();
            assertThat(dto.id()).isEqualTo(usuario.getId());
            assertThat(dto.cpf()).isEqualTo(usuario.getCpf());
            assertThat(dto.nome()).isEqualTo(usuario.getNome());

            verify(usuarioRepository).findById(1L);
            verify(usuarioRepository, never()).save(any());
            verifyNoMoreInteractions(usuarioRepository);
        }

        @Test
        @DisplayName("deve lançar 404 quando o usuário não existe")
        void deveLancar404AoBuscarPerfil() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.buscarPerfil(1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode()).isEqualTo(NOT_FOUND);
                    assertThat(ex.getReason()).isEqualTo("Usuário não encontrado");
                });

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar 403 quando o usuário está desativado")
        void deveLancar403QuandoUsuarioDesativadoAoBuscarPerfil() {
            usuario.setAtivo(false);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

            assertThatThrownBy(() -> usuarioService.buscarPerfil(1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode()).isEqualTo(FORBIDDEN);
                    assertThat(ex.getReason()).isEqualTo("Usuário desativado");
                });

            verify(usuarioRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("buscarUsuario")
    class BuscarUsuario {

        @Test
        @DisplayName("deve retornar o usuário público quando existe e está ativo")
        void deveBuscarUsuarioPublico() {
            when(usuarioRepository.findByMatricula("20230001")).thenReturn(Optional.of(usuario));

            UsuarioPublicoDTO dto = usuarioService.buscarUsuario("20230001");

            assertThat(dto).isNotNull();
            assertThat(dto.id()).isEqualTo(usuario.getId());
            assertThat(dto.nome()).isEqualTo(usuario.getNome());
            assertThat(dto.email()).isEqualTo(usuario.getEmail());
            assertThat(dto.curso()).isEqualTo(usuario.getCurso());

            verify(usuarioRepository).findByMatricula("20230001");
            verify(usuarioRepository, never()).save(any());
            verifyNoMoreInteractions(usuarioRepository);
        }

        @Test
        @DisplayName("deve lançar 404 quando a matrícula não existe")
        void deveLancar404AoBuscarUsuarioPublico() {
            when(usuarioRepository.findByMatricula("20230001")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.buscarUsuario("20230001"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode()).isEqualTo(NOT_FOUND);
                    assertThat(ex.getReason()).isEqualTo("Usuário não encontrado");
                });
        }

        @Test
        @DisplayName("deve lançar 403 quando o usuário está desativado")
        void deveLancar403QuandoUsuarioDesativadoAoBuscarUsuarioPublico() {
            usuario.setAtivo(false);
            when(usuarioRepository.findByMatricula("20230001")).thenReturn(Optional.of(usuario));

            assertThatThrownBy(() -> usuarioService.buscarUsuario("20230001"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode()).isEqualTo(FORBIDDEN);
                    assertThat(ex.getReason()).isEqualTo("Usuário desativado");
                });
        }
    }

    @Nested
    @DisplayName("atualizarPerfil")
    class AtualizarPerfil {

        @Test
        @DisplayName("deve atualizar gênero e receberEmail quando ambos são informados")
        void deveAtualizarGeneroEReceberEmail() {
            UpdatePerfilRequestDTO request = new UpdatePerfilRequestDTO(Genero.MASCULINO, false);

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            mockSaveRetornandoMesmoUsuario();

            UsuarioDTO dto = usuarioService.atualizarPerfil(1L, request);

            assertThat(dto.genero()).isEqualTo(Genero.MASCULINO.name());
            assertThat(dto.receberEmail()).isFalse();

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            assertThat(captor.getValue().getGenero()).isEqualTo(Genero.MASCULINO);
            assertThat(captor.getValue().getReceberEmail()).isFalse();
        }

        @Test
        @DisplayName("deve atualizar somente o gênero quando receberEmail é null")
        void deveAtualizarSomenteGenero() {
            UpdatePerfilRequestDTO request = new UpdatePerfilRequestDTO(Genero.FEMININO, null);

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            mockSaveRetornandoMesmoUsuario();

            usuarioService.atualizarPerfil(1L, request);

            assertThat(usuario.getGenero()).isEqualTo(Genero.FEMININO);
            assertThat(usuario.getReceberEmail()).isTrue();

            verify(usuarioRepository).save(usuario);
        }

        @Test
        @DisplayName("deve atualizar somente receberEmail quando gênero é null")
        void deveAtualizarSomenteReceberEmail() {
            UpdatePerfilRequestDTO request = new UpdatePerfilRequestDTO(null, false);

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            mockSaveRetornandoMesmoUsuario();

            usuarioService.atualizarPerfil(1L, request);

            assertThat(usuario.getReceberEmail()).isFalse();
            assertThat(usuario.getGenero()).isEqualTo(Genero.NAO_INFORMADO);

            verify(usuarioRepository).save(usuario);
        }

        @Test
        @DisplayName("não deve alterar nada quando gênero e receberEmail são null, mas ainda deve salvar")
        void naoDeveAlterarNadaQuandoAmbosSaoNull() {
            UpdatePerfilRequestDTO request = new UpdatePerfilRequestDTO(null, null);

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            mockSaveRetornandoMesmoUsuario();

            UsuarioDTO dto = usuarioService.atualizarPerfil(1L, request);

            assertThat(usuario.getGenero()).isEqualTo(Genero.NAO_INFORMADO);
            assertThat(usuario.getReceberEmail()).isTrue();
            assertThat(dto.genero()).isEqualTo(Genero.NAO_INFORMADO.name());
            assertThat(dto.receberEmail()).isTrue();

            verify(usuarioRepository).save(usuario);
        }

        @ParameterizedTest(name = "deve aceitar o gênero {0}")
        @EnumSource(Genero.class)
        @DisplayName("deve atualizar corretamente para qualquer valor de Genero")
        void deveAtualizarParaQualquerGenero(Genero genero) {
            UpdatePerfilRequestDTO request = new UpdatePerfilRequestDTO(genero, null);

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            mockSaveRetornandoMesmoUsuario();

            UsuarioDTO dto = usuarioService.atualizarPerfil(1L, request);

            assertThat(dto.genero()).isEqualTo(genero.name());
            assertThat(usuario.getGenero()).isEqualTo(genero);
        }

        @Test
        @DisplayName("deve lançar 404 quando o usuário não existe")
        void deveLancar404AoAtualizarPerfil() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.atualizarPerfil(1L, new UpdatePerfilRequestDTO(null, true)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode()).isEqualTo(NOT_FOUND);
                });

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar 403 ao tentar atualizar perfil de usuário desativado")
        void deveLancar403AoAtualizarPerfilDeUsuarioDesativado() {
            usuario.setAtivo(false);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

            assertThatThrownBy(() -> usuarioService.atualizarPerfil(1L, new UpdatePerfilRequestDTO(null, true)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode()).isEqualTo(FORBIDDEN);
                });

            verify(usuarioRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("desativarPerfil")
    class DesativarPerfil {

        @Test
        @DisplayName("deve desativar o usuário ativo")
        void deveDesativarPerfil() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

            usuarioService.desativarPerfil(1L);

            assertThat(usuario.getAtivo()).isFalse();

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            assertThat(captor.getValue().getAtivo()).isFalse();
            assertThat(captor.getValue().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("deve lançar 404 quando o usuário não existe")
        void deveLancar404AoDesativarPerfil() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.desativarPerfil(1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode()).isEqualTo(NOT_FOUND);
                });

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar 403 ao tentar desativar usuário já desativado")
        void deveLancar403AoDesativarUsuarioJaDesativado() {
            usuario.setAtivo(false);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

            assertThatThrownBy(() -> usuarioService.desativarPerfil(1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode()).isEqualTo(FORBIDDEN);
                    assertThat(ex.getReason()).isEqualTo("Usuário desativado");
                });

            verify(usuarioRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("perfilPublico")
    class PerfilPublico {

        @Test
        @DisplayName("deve retornar o perfil público correto com reputação quando o usuário existe")
        void deveRetornarPerfilPublico() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

            ReputacaoDTO reputacaoSimulada =
                    new ReputacaoDTO(1L, 4.8, 15L);
            when(avaliacaoService.buscarReputacao(1L)).thenReturn(reputacaoSimulada);

            PerfilUsuarioDTO dto = usuarioService.perfilPublico(1L);

            assertThat(dto).isNotNull();
            assertThat(dto.id()).isEqualTo(usuario.getId());
            assertThat(dto.nome()).isEqualTo(usuario.getNome());
            assertThat(dto.curso()).isEqualTo(usuario.getCurso());
            assertThat(dto.genero()).isEqualTo(usuario.getGenero().name());
            assertThat(dto.reputacao()).isEqualTo(4.8);
            assertThat(dto.quantidadeAvaliacoes()).isEqualTo(15);

            verify(usuarioRepository).findById(1L);
            verify(avaliacaoService).buscarReputacao(1L);
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar 404 quando o usuário consultado não existe")
        void deveLancar404AoBuscarPerfilPublicoInexistente() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.perfilPublico(1L))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(exception -> {
                        ResponseStatusException ex = (ResponseStatusException) exception;
                        assertThat(ex.getStatusCode()).isEqualTo(NOT_FOUND);
                        assertThat(ex.getReason()).isEqualTo("Usuário não encontrado");
                    });

            verify(usuarioRepository).findById(1L);
            verify(avaliacaoService, never()).buscarReputacao(any());
            verify(usuarioRepository, never()).save(any());
        }
    }
}