package com.unicar.repository;

import com.unicar.domain.ReservaCarona;
import com.unicar.enums.StatusReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservaCaronaRepository extends JpaRepository<ReservaCarona, Long> {
    int countByCarona_IdAndStatus(Long caronaId, StatusReserva status);
    List<ReservaCarona> findByCaronaIdAndStatusIn(Long caronaId, List<StatusReserva> statusList);
    List<ReservaCarona> findByCaronaIdAndStatus(Long caronaId,StatusReserva status);
    List<ReservaCarona> findByCaronaId(Long caronaId);
    boolean existsByCarona_IdAndUsuario_IdAndStatusIn(Long caronaId, Long usuarioId, List<StatusReserva> statusList);
    List<ReservaCarona> findByUsuario_Id(Long usuarioId);
    List<ReservaCarona> findByCarona_Motorista_Id(Long motoristaId);
    boolean existsByCaronaIdAndUsuarioId(Long caronaId, Long usuarioId);
}