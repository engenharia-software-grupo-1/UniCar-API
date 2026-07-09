package com.unicar.repository;

import com.unicar.domain.ReservaCarona;
import com.unicar.enums.StatusReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservaCaronaRepository extends JpaRepository<ReservaCarona, Long> {

    List<ReservaCarona> findByCaronaIdAndStatus(Long caronaId,StatusReserva status);
    List<ReservaCarona> findByCaronaId(Long caronaId);
}