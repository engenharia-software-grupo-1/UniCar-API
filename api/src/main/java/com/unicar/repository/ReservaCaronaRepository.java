package com.unicar.repository;

import com.unicar.domain.ReservaCarona;
import com.unicar.enums.StatusReserva;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservaCaronaRepository extends JpaRepository<ReservaCarona, Long> {
    @Query("select coalesce(sum(r.quantidadePassageiros), 0) from ReservaCarona r where r.carona.id = :caronaId and r.status = :status")
    int somarPassageirosPorCaronaEStatus(@Param("caronaId") Long caronaId, @Param("status") StatusReserva status);

    @Query("select coalesce(sum(r.quantidadePassageiros), 0) from ReservaCarona r where r.carona.id = :caronaId and r.status in :statusList")
    int somarPassageirosPorCaronaEStatusIn(@Param("caronaId") Long caronaId, @Param("statusList") List<StatusReserva> statusList);

    List<ReservaCarona> findByCaronaIdAndStatusIn(Long caronaId, List<StatusReserva> statusList);

    List<ReservaCarona> findByCaronaIdAndStatus(Long caronaId,StatusReserva status);

    boolean existsByCarona_IdAndUsuario_IdAndStatusIn(Long caronaId, Long usuarioId, List<StatusReserva> statusList);

    List<ReservaCarona> findByUsuario_Id(Long usuarioId);

    List<ReservaCarona> findByCarona_Motorista_Id(Long motoristaId);

    boolean existsByCaronaIdAndUsuarioId(Long caronaId, Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservaCarona r where r.id = :id")
    Optional<ReservaCarona> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r
              from ReservaCarona r
             where r.status = com.unicar.enums.StatusReserva.PENDENTE
               and r.dataExpiracao <= :agora
            """)
    List<ReservaCarona> buscarReservasPendentesExpiradasParaAtualizacao(@Param("agora") LocalDateTime agora);

    @Query("SELECT r FROM ReservaCarona r " +
            "JOIN r.carona c " +
            "WHERE r.usuario.id = :passageiroId " +
            "AND c.status = com.unicar.enums.StatusCarona.FINALIZADA " +
            "ORDER BY c.dataHoraPartida DESC")
    Page<ReservaCarona> findHistoricoComoPassageiro(@Param("passageiroId") Long passageiroId, Pageable pageable);

    long countByUsuarioIdAndStatus(
            Long usuarioId,
            StatusReserva status
    );
}
