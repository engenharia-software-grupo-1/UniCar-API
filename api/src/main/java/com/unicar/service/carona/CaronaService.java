package com.unicar.service.carona;

import com.unicar.domain.Carona;
import com.unicar.domain.ReservaCarona;
import com.unicar.dto.carona.PassageiroResponseDTO;
import com.unicar.enums.StatusCarona;
import com.unicar.enums.StatusReserva;
import com.unicar.exception.AcessoNegadoException;
import com.unicar.exception.CaronaNaoEncontradaException;
import com.unicar.exception.EstadoInvalidoException;
import com.unicar.repository.CaronaRepository;
import com.unicar.repository.ReservaCaronaRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CaronaService {

    private final CaronaRepository caronaRepository;
    private final ReservaCaronaRepository reservaCaronaRepository;

    public List<PassageiroResponseDTO> listarPassageiros(Long caronaId, Long usuarioId) {
        Carona carona = buscarCarona(caronaId);
        validarMotorista(carona, usuarioId);
        List<ReservaCarona> reservas = reservaCaronaRepository.findByCaronaIdAndStatus(caronaId, StatusReserva.ACEITA);

        return reservas.stream()
                .map(PassageiroResponseDTO::from)
                .toList();
    }

    @Transactional
    public void iniciarCarona(Long caronaId, Long usuarioId) {
        Carona carona = buscarCarona(caronaId);
        validarMotorista(carona, usuarioId);

        if (carona.getStatus() != StatusCarona.EM_ANDAMENTO) {
            throw new EstadoInvalidoException("Só é possível iniciar caronas com status AGENDADA. Status atual: " + carona.getStatus());
        }

        carona.setStatus(StatusCarona.EM_ANDAMENTO);
        caronaRepository.save(carona);
    }

    @Transactional
    public void finalizarCarona(Long caronaId, Long usuarioId) {
        Carona carona = buscarCarona(caronaId);
        validarMotorista(carona, usuarioId);

        if (carona.getStatus() != StatusCarona.EM_ANDAMENTO) {
            throw new EstadoInvalidoException("Só é possível finalizar caronas com status EM_ANDAMENTO. Status atual: " + carona.getStatus());
        }

        carona.setStatus(StatusCarona.FINALIZADA);
        caronaRepository.save(carona);

        List<ReservaCarona> reservas = reservaCaronaRepository
                .findByCaronaIdAndStatus(caronaId, StatusReserva.ACEITA);

        reservas.forEach(r -> r.setStatus(StatusReserva.CONCLUIDA));
        reservaCaronaRepository.saveAll(reservas);
    }

    private Carona buscarCarona(Long id) {
        return caronaRepository.findById(id)
                .orElseThrow(() -> new CaronaNaoEncontradaException("Carona não encontrada: id=" + id));
    }

    private void validarMotorista(Carona carona, Long usuarioId) {
        if (!carona.getMotorista().getId().equals(usuarioId)) {
            throw new AcessoNegadoException("Usuário não é o motorista desta carona");
        }
    }
}