package com.unicar.service.carona;

import com.unicar.domain.ReservaCarona;
import com.unicar.domain.Usuario;
import com.unicar.enums.StatusReserva;
import com.unicar.exception.AcessoNegadoException;
import com.unicar.exception.EstadoInvalidoException;
import com.unicar.exception.ReservaNaoEncontradaException;
import com.unicar.repository.ReservaCaronaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class ReservaCaronaServiceTest {
    @Mock
    private ReservaCaronaRepository repository;

    @InjectMocks
    private ReservaCaronaService service;

    private ReservaCarona reserva;
    private Usuario usuario;

    private final Long reservaId = 1L;
    private final Long usuarioId = 10L;
    private final Long outroUsuarioId = 20L;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        usuario = new Usuario();
        usuario.setId(usuarioId);


        reserva = new ReservaCarona();
        reserva.setId(reservaId);
        reserva.setUsuario(usuario);
    }

    @Nested
    @DisplayName("Remover reserva")
    class RemoverReserva {

        @Test
        @DisplayName("Deve remover reserva pendente")
        void deveRemoverReservaPendente() {


            reserva.setStatus(StatusReserva.PENDENTE);


            when(repository.findById(reservaId))
                    .thenReturn(Optional.of(reserva));


            service.removerReserva(
                    reservaId,
                    usuarioId
            );


            assertEquals(
                    StatusReserva.CANCELADA,
                    reserva.getStatus()
            );


            verify(repository)
                    .save(reserva);
        }

        @Test
        @DisplayName("Deve remover reserva aceita")
        void deveRemoverReservaAceita() {


            reserva.setStatus(StatusReserva.ACEITA);


            when(repository.findById(reservaId))
                    .thenReturn(Optional.of(reserva));


            service.removerReserva(
                    reservaId,
                    usuarioId
            );


            assertEquals(
                    StatusReserva.CANCELADA,
                    reserva.getStatus()
            );


            verify(repository)
                    .save(reserva);
        }

        @Test
        @DisplayName("Não deve remover reserva com status inválido")
        void naoDeveRemoverReservaComStatusInvalido() {


            reserva.setStatus(StatusReserva.CANCELADA);


            when(repository.findById(reservaId))
                    .thenReturn(Optional.of(reserva));

            assertThrows(
                    EstadoInvalidoException.class,
                    () ->
                            service.removerReserva(
                                    reservaId,
                                    usuarioId
                            )
            );


            verify(repository, never())
                    .save(any());
        }

        @Test
        @DisplayName("Não deve remover reserva de outro usuário")
        void naoDeveRemoverReservaDeOutroUsuario() {


            reserva.setStatus(StatusReserva.PENDENTE);


            when(repository.findById(reservaId))
                    .thenReturn(Optional.of(reserva));


            assertThrows(
                    AcessoNegadoException.class,
                    () ->
                            service.removerReserva(
                                    reservaId,
                                    outroUsuarioId
                            )
            );


            verify(repository, never())
                    .save(any());
        }
    }

    @Nested
    @DisplayName("Buscar reserva")
    class BuscarReserva {
        @Test
        @DisplayName("Deve buscar reserva existente")
        void deveBuscarReserva() {
            when(repository.findById(reservaId))
                    .thenReturn(Optional.of(reserva));

            ReservaCarona resultado =
                    service.buscarReserva(reservaId);

            assertEquals(
                    reserva,
                    resultado
            );

            verify(repository)
                    .findById(reservaId);
        }

        @Test
        @DisplayName("Deve lançar erro quando reserva não existir")
        void deveLancarErroQuandoReservaNaoExistir() {

            when(repository.findById(reservaId))
                    .thenReturn(Optional.empty());

            assertThrows(ReservaNaoEncontradaException.class, () ->service.buscarReserva(reservaId));
        }
    }
}