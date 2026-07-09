package com.unicar.controller.carona;

import com.unicar.domain.Usuario;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.carona.ReservaCaronaService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;


class ReservaCaronaControllerTest {

    @Mock
    private ReservaCaronaService reservaCaronaService;

    @InjectMocks
    private ReservaCaronaController reservaCaronaController;


    private UsuarioDetails usuarioDetails;

    private final Long usuarioId = 1L;
    private final Long reservaId = 10L;


    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setEmail("teste@email.com");

        usuarioDetails = new UsuarioDetails(usuario);
    }


    @Nested
    @DisplayName("PATCH /reservas/{id}/cancelar")
    class CancelarReserva {


        @Test
        @DisplayName("Deve cancelar uma reserva de carona")
        void deveCancelarReserva() {

            ResponseEntity<Void> response =
                    reservaCaronaController.cancelar(
                            reservaId,
                            usuarioDetails
                    );


            assertEquals(
                    HttpStatus.NO_CONTENT,
                    response.getStatusCode()
            );


            verify(reservaCaronaService)
                    .cancelarReserva(reservaId, usuarioId);
        }
    }
}