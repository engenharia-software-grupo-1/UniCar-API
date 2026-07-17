package com.unicar.repository;

import com.unicar.domain.ReservaCarona;
import com.unicar.enums.StatusReserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    @Query("SELECT r FROM ReservaCarona r " +
            "JOIN r.carona c " +
            "WHERE r.usuario.id = :passageiroId " +
            "AND c.status = com.unicar.enums.StatusCarona.FINALIZADA " +
            "ORDER BY c.dataHoraPartida DESC")
    Page<ReservaCarona> findHistoricoComoPassageiro(@Param("passageiroId") Long passageiroId, Pageable pageable);
}