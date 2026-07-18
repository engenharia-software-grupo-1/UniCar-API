package com.unicar.service;

import com.unicar.domain.InteresseTrajeto;
import com.unicar.dto.interesseTrajeto.CoordenadaDTO;
import com.unicar.dto.interesseTrajeto.InteresseTrajetoRequest;
import com.unicar.exception.InteresseNaoEncontrado;
import com.unicar.exception.RegraDeNegocioException;
import com.unicar.repository.InteresseTrajetoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InteresseTrajetoServiceTest {

    @Mock
    private InteresseTrajetoRepository repository;

    @InjectMocks
    private InteresseTrajetoService service;

    private final Long usuarioId = 1L;

    private InteresseTrajetoRequest request() {
        return new InteresseTrajetoRequest(
                new CoordenadaDTO(
                        new BigDecimal("-7.21456"),
                        new BigDecimal("-35.90872")),
                new CoordenadaDTO(
                        new BigDecimal("-7.21590"),
                        new BigDecimal("-35.90950"))
        );
    }

    @Nested
    @DisplayName("Cadastrar")
    class Cadastrar {

        @Test
        @DisplayName("deve cadastrar interesse")
        void deveCadastrar() {

            when(repository.existsByUsuarioIdAndOrigemLatitudeAndOrigemLongitudeAndDestinoLatitudeAndDestinoLongitude(
                    any(), any(), any(), any(), any()))
                    .thenReturn(false);

            InteresseTrajeto interesse = InteresseTrajeto.builder()
                    .id(10L)
                    .build();

            when(repository.save(any()))
                    .thenReturn(interesse);

            var dto = service.cadastrar(usuarioId, request());

            assertEquals(10L, dto.id());

            verify(repository).save(any());
        }

        @Test
        @DisplayName("não deve permitir interesse duplicado")
        void naoDevePermitirDuplicado() {

            when(repository.existsByUsuarioIdAndOrigemLatitudeAndOrigemLongitudeAndDestinoLatitudeAndDestinoLongitude(
                    any(), any(), any(), any(), any()))
                    .thenReturn(true);

            assertThrows(RegraDeNegocioException.class,
                    () -> service.cadastrar(usuarioId, request()));

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Listar")
    class Listar {

        @Test
        @DisplayName("deve listar interesses")
        void deveListar() {

            InteresseTrajeto interesse = InteresseTrajeto.builder()
                    .id(1L)
                    .origemLatitude(new BigDecimal("-7.21456"))
                    .origemLongitude(new BigDecimal("-35.90872"))
                    .destinoLatitude(new BigDecimal("-7.21590"))
                    .destinoLongitude(new BigDecimal("-35.90950"))
                    .build();

            when(repository.findByUsuarioId(usuarioId))
                    .thenReturn(List.of(interesse));

            var lista = service.listar(usuarioId);

            assertEquals(1, lista.size());
            assertEquals(1L, lista.getFirst().id());

            verify(repository).findByUsuarioId(usuarioId);
        }
    }

    @Nested
    @DisplayName("Remover")
    class Remover {

        @Test
        @DisplayName("deve remover interesse")
        void deveRemover() {

            InteresseTrajeto interesse = InteresseTrajeto.builder()
                    .id(1L)
                    .build();

            when(repository.findByIdAndUsuarioId(1L, usuarioId))
                    .thenReturn(Optional.of(interesse));

            service.remover(usuarioId, 1L);

            verify(repository).delete(interesse);
        }

        @Test
        @DisplayName("deve lançar exceção quando interesse não existir")
        void deveLancarExcecao() {

            when(repository.findByIdAndUsuarioId(1L, usuarioId))
                    .thenReturn(Optional.empty());

            assertThrows(InteresseNaoEncontrado.class,
                    () -> service.remover(usuarioId, 1L));

            verify(repository, never()).delete(any());
        }
    }
}