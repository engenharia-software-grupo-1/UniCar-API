package com.unicar.controller.carona;

import com.unicar.domain.Usuario;
import com.unicar.dto.carona.PassageiroResponseDTO;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.carona.CaronaService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CaronaControllerTest {

    @Mock
    private CaronaService caronaService;

    @InjectMocks
    private CaronaController caronaController;
    private UsuarioDetails usuarioDetails;

    private final Long usuarioId = 1L;
    private final Long caronaId = 10L;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setEmail("teste@email.com");

        usuarioDetails = new UsuarioDetails(usuario);
    }

    @Nested
    @DisplayName("GET /caronas/{id}/passageiros")
    class ListarPassageiros {

        @Test
        @DisplayName("Deve listar passageiros da carona")
        void deveListarPassageiros() {

            List<PassageiroResponseDTO> passageiros = List.of(
                    new PassageiroResponseDTO(
                            1L,
                            usuarioId,
                            "João",
                            5
                    )
            );


            when(caronaService.listarPassageiros(caronaId, usuarioId))
                    .thenReturn(passageiros);


            ResponseEntity<List<PassageiroResponseDTO>> response =
                    caronaController.listarPassageiros(
                            caronaId,
                            usuarioDetails
                    );


            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(passageiros, response.getBody());


            verify(caronaService)
                    .listarPassageiros(caronaId, usuarioId);
        }
    }

    @Nested
    @DisplayName("PATCH /caronas/{id}/iniciar")
    class IniciarCarona {

        @Test
        @DisplayName("Deve iniciar uma carona")
        void deveIniciarCarona() {


            ResponseEntity<Void> response =
                    caronaController.iniciar(
                            caronaId,
                            usuarioDetails
                    );


            assertEquals(
                    HttpStatus.NO_CONTENT,
                    response.getStatusCode()
            );


            verify(caronaService)
                    .iniciarCarona(caronaId, usuarioId);
        }
    }

    @Nested
    @DisplayName("PATCH /caronas/{id}/finalizar")
    class FinalizarCarona {
        @Test
        @DisplayName("Deve finalizar uma carona")
        void deveFinalizarCarona() {


            ResponseEntity<Void> response =
                    caronaController.finalizar(
                            caronaId,
                            usuarioDetails
                    );


            assertEquals(
                    HttpStatus.NO_CONTENT,
                    response.getStatusCode()
            );


            verify(caronaService)
                    .finalizarCarona(caronaId, usuarioId);
        }
    }
}