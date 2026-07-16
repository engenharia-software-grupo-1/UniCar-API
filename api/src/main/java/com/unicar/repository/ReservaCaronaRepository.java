package com.unicar.repository;

import com.unicar.domain.ReservaCarona;
import com.unicar.enums.StatusReserva;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservaCaronaRepository extends JpaRepository<ReservaCarona, Long> {
    int countByCarona_IdAndStatus(Long caronaId, StatusReserva status);
    List<ReservaCarona> findByCaronaIdAndStatusIn(Long caronaId, List<StatusReserva> statusList);
    List<ReservaCarona> findByCaronaIdAndStatus(Long caronaId,StatusReserva status);
    List<ReservaCarona> findByCaronaId(Long caronaId);
    boolean existsByCarona_IdAndUsuario_IdAndStatusIn(Long caronaId, Long usuarioId, List<StatusReserva> statusList);
    List<ReservaCarona> findByUsuario_Id(Long usuarioId);
    List<ReservaCarona> findByCarona_Motorista_Id(Long motoristaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservaCarona r where r.id = :id")
    Optional<ReservaCarona> findByIdForUpdate(@Param("id") Long id);
}