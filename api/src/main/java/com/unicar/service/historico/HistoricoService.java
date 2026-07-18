package com.unicar.service.historico;

import com.unicar.domain.Carona;
import com.unicar.domain.ReservaCarona;
import com.unicar.dto.historico.DetalhesHistoricoResponseDTO;
import com.unicar.dto.historico.HistoricoMotoristaResponseDTO;
import com.unicar.dto.historico.HistoricoPassageiroResponseDTO;
import com.unicar.dto.historico.ParticipanteResumoDTO;
import com.unicar.enums.StatusReserva;
import com.unicar.repository.CaronaRepository;
import com.unicar.repository.ReservaCaronaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoricoService {

    private final CaronaRepository caronaRepository;
    private final ReservaCaronaRepository reservaCaronaRepository;

    @Transactional(readOnly = true)
    public Page<HistoricoMotoristaResponseDTO> listarHistoricoComoMotorista(Long usuarioId, Pageable pageable) {
        Page<Carona> caronas = caronaRepository.findHistoricoComoMotorista(usuarioId, pageable);
        return caronas.map(carona -> {
            int totalPassageiros = reservaCaronaRepository.countByCarona_IdAndStatus(carona.getId(), StatusReserva.ACEITA);

            return new HistoricoMotoristaResponseDTO(
                    carona.getId(),
                    carona.getOrigemDescricao(),
                    carona.getDestinoDescricao(),
                    carona.getStatus(),
                    carona.getDataHoraPartida(),
                    totalPassageiros
            );
        });
    }

    @Transactional(readOnly = true)
    public Page<HistoricoPassageiroResponseDTO> listarHistoricoComoPassageiro(Long usuarioId, Pageable pageable) {
        Page<ReservaCarona> reservas = reservaCaronaRepository.findHistoricoComoPassageiro(usuarioId, pageable);
        return reservas.map(reserva -> {
            Carona carona = reserva.getCarona();
            int quantPassageiros = reservaCaronaRepository.countByCarona_IdAndStatus(carona.getId(), StatusReserva.ACEITA);

            return new HistoricoPassageiroResponseDTO(
                    reserva.getId(),
                    carona.getId(),
                    carona.getOrigemDescricao(),
                    carona.getDestinoDescricao(),
                    carona.getMotorista().getNome(),
                    carona.getStatus(),
                    carona.getDataHoraPartida(),
                    quantPassageiros
            );
        });
    }

    @Transactional(readOnly = true)
    public DetalhesHistoricoResponseDTO obterDetalhesViagem(Long caronaId, Long usuarioId) {
        Carona carona = caronaRepository.findById(caronaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Histórico ou carona não encontrada."));

        boolean isMotorista = carona.getMotorista().getId().equals(usuarioId);

        boolean isPassageiro = reservaCaronaRepository.existsByCaronaIdAndUsuarioId(caronaId, usuarioId);

        if (!isMotorista && !isPassageiro) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: você não participou desta viagem.");
        }

        ParticipanteResumoDTO motoristaDTO = new ParticipanteResumoDTO(
                carona.getMotorista().getId(),
                carona.getMotorista().getNome()
        );

        List<ReservaCarona> reservasAceitas = reservaCaronaRepository.findByCaronaIdAndStatus(carona.getId(), StatusReserva.ACEITA);

        List<ParticipanteResumoDTO> passageirosDTO = reservasAceitas.stream()
                .map(r -> new ParticipanteResumoDTO(r.getUsuario().getId(), r.getUsuario().getNome()))
                .toList();

        return new DetalhesHistoricoResponseDTO(
                carona.getId(),
                carona.getOrigemDescricao(),
                carona.getDestinoDescricao(),
                motoristaDTO,
                carona.getStatus(),
                carona.getDataHoraPartida(),
                passageirosDTO
        );
    }
}