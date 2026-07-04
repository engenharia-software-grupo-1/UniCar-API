package com.unicar.service.usuario;

import com.unicar.domain.Usuario;
import com.unicar.dto.usuario.UpdatePerfilRequestDTO;
import com.unicar.dto.usuario.UsuarioDTO;
import com.unicar.dto.usuario.UsuarioPublicoDTO;
import com.unicar.enums.Genero;
import com.unicar.repository.UsuarioRepository;
import com.unicar.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

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

    @Test
    void deveBuscarPerfil() {
        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(usuario));

        UsuarioDTO dto = usuarioService.buscarPerfil(1L);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(usuario.getId());
        assertThat(dto.cpf()).isEqualTo(usuario.getCpf());
        assertThat(dto.nome()).isEqualTo(usuario.getNome());

        verify(usuarioRepository).findById(1L);
    }

    @Test
    void deveBuscarUsuarioPublico() {
        when(usuarioRepository.findByMatricula("20230001"))
                .thenReturn(Optional.of(usuario));

        UsuarioPublicoDTO dto = usuarioService.buscarUsuario("20230001");

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(usuario.getId());
        assertThat(dto.nome()).isEqualTo(usuario.getNome());
        assertThat(dto.email()).isEqualTo(usuario.getEmail());
        assertThat(dto.curso()).isEqualTo(usuario.getCurso());

        verify(usuarioRepository).findByMatricula("20230001");
    }

    @Test
    void deveAtualizarGeneroEReceberEmail() {

        UpdatePerfilRequestDTO request =
                new UpdatePerfilRequestDTO(Genero.MASCULINO, false);

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(usuario));

        when(usuarioRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioDTO dto = usuarioService.atualizarPerfil(1L, request);

        assertThat(dto.genero()).isEqualTo("MASCULINO");
        assertThat(dto.receberEmail()).isFalse();

        verify(usuarioRepository).save(usuario);
    }

    @Test
    void deveAtualizarSomenteGenero() {

        UpdatePerfilRequestDTO request =
                new UpdatePerfilRequestDTO(Genero.FEMININO, null);

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(usuario));

        when(usuarioRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        usuarioService.atualizarPerfil(1L, request);

        assertThat(usuario.getGenero())
                .isEqualTo(Genero.FEMININO);

        assertThat(usuario.getReceberEmail())
                .isTrue();

        verify(usuarioRepository).save(usuario);
    }

    @Test
    void deveAtualizarSomenteReceberEmail() {

        UpdatePerfilRequestDTO request =
                new UpdatePerfilRequestDTO(null, false);

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(usuario));

        when(usuarioRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        usuarioService.atualizarPerfil(1L, request);

        assertThat(usuario.getReceberEmail())
                .isFalse();

        assertThat(usuario.getGenero())
                .isEqualTo(Genero.NAO_INFORMADO);

        verify(usuarioRepository).save(usuario);
    }

    @Test
    void deveDesativarPerfil() {

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(usuario));

        usuarioService.desativarPerfil(1L);

        assertThat(usuario.getAtivo()).isFalse();

        verify(usuarioRepository).save(usuario);
    }

    @Test
    void deveLancar404AoBuscarPerfil() {

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResponseStatusException ex =
                catchThrowableOfType(
                        () -> usuarioService.buscarPerfil(1L),
                        ResponseStatusException.class);

        assertThat(ex.getStatusCode())
                .isEqualTo(NOT_FOUND);
    }

    @Test
    void deveLancar404AoBuscarUsuarioPublico() {

        when(usuarioRepository.findByMatricula("20230001"))
                .thenReturn(Optional.empty());

        ResponseStatusException ex =
                catchThrowableOfType(
                        () -> usuarioService.buscarUsuario("20230001"),
                        ResponseStatusException.class);

        assertThat(ex.getStatusCode())
                .isEqualTo(NOT_FOUND);
    }

    @Test
    void deveLancar403QuandoUsuarioDesativadoAoBuscarPerfil() {

        usuario.setAtivo(false);

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(usuario));

        ResponseStatusException ex =
                catchThrowableOfType(
                        () -> usuarioService.buscarPerfil(1L),
                        ResponseStatusException.class);

        assertThat(ex.getStatusCode())
                .isEqualTo(FORBIDDEN);
    }

    @Test
    void deveLancar403QuandoUsuarioDesativadoAoBuscarUsuarioPublico() {

        usuario.setAtivo(false);

        when(usuarioRepository.findByMatricula("20230001"))
                .thenReturn(Optional.of(usuario));

        ResponseStatusException ex =
                catchThrowableOfType(
                        () -> usuarioService.buscarUsuario("20230001"),
                        ResponseStatusException.class);

        assertThat(ex.getStatusCode())
                .isEqualTo(FORBIDDEN);
    }

    @Test
    void deveLancar403AoAtualizarPerfilDeUsuarioDesativado() {

        usuario.setAtivo(false);

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(usuario));

        ResponseStatusException ex =
                catchThrowableOfType(
                        () -> usuarioService.atualizarPerfil(
                                1L,
                                new UpdatePerfilRequestDTO(null, true)),
                        ResponseStatusException.class);

        assertThat(ex.getStatusCode())
                .isEqualTo(FORBIDDEN);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveLancar403AoDesativarUsuarioJaDesativado() {

        usuario.setAtivo(false);

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(usuario));

        ResponseStatusException ex =
                catchThrowableOfType(
                        () -> usuarioService.desativarPerfil(1L),
                        ResponseStatusException.class);

        assertThat(ex.getStatusCode())
                .isEqualTo(FORBIDDEN);

        verify(usuarioRepository, never()).save(any());
    }
}