package com.unicar.service.carona;

import com.unicar.domain.ReservaCarona;
import com.unicar.enums.StatusReserva;
import com.unicar.exception.AcessoNegadoException;
import com.unicar.exception.EstadoInvalidoException;
import com.unicar.exception.ReservaNaoEncontradaException;
import com.unicar.repository.ReservaCaronaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservaCaronaService {

    private final ReservaCaronaRepository repository;

    @Transactional
    public void cancelarReserva(Long reservaId, Long usuarioId) {
        ReservaCarona reserva = buscarReserva(reservaId);
        validarDono(reserva, usuarioId);

        if (reserva.getStatus() != StatusReserva.PENDENTE && reserva.getStatus() != StatusReserva.ACEITA) {
            throw new EstadoInvalidoException("Não é possível cancelar uma reserva com status " + reserva.getStatus());
        }

        reserva.setStatus(StatusReserva.CANCELADA);
        repository.save(reserva);
    }

    public ReservaCarona buscarReserva(Long reservaId) {
        return repository.findById(reservaId).orElseThrow(() -> new ReservaNaoEncontradaException("Reserva não encontrada: id=" + reservaId));
    }

    private void validarDono(ReservaCarona reserva, Long usuarioId) {
        if (!reserva.getUsuario().getId().equals(usuarioId)) {
            throw new AcessoNegadoException("Usuário não é o dono desta reserva");
        }
    }
}