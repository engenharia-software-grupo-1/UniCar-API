package com.unicar.repository;

import com.unicar.domain.Carona;
import com.unicar.enums.StatusCarona;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CaronaRepository extends JpaRepository<Carona, Long>, JpaSpecificationExecutor<Carona> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Carona c where c.id = :id")
    Optional<Carona> findByIdForUpdate(@Param("id") Long id);

    boolean existsByMotorista_IdAndStatus(Long motoristaId, StatusCarona status);

    List<Carona> findByMotorista_Id(Long motoristaId);

    @Query(value = """
            SELECT
                c.*,
                earth_distance(
                    ll_to_earth(:latitude, :longitude),
                    ll_to_earth(c.origem_latitude, c.origem_longitude)
                ) / 1000 AS distancia_km
            FROM carona c
            WHERE c.status = 'CRIADA'
              AND c.data_hora_partida > CURRENT_TIMESTAMP
              AND earth_distance(
                    ll_to_earth(:latitude, :longitude),
                    ll_to_earth(c.origem_latitude, c.origem_longitude)
                  ) <= :raioMetros
            ORDER BY distancia_km ASC
            """, nativeQuery = true)
    List<CaronaProximaProjection> buscarCaronasProximas(
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("raioMetros") double raioMetros);

    @Query("SELECT c FROM Carona c " +
        "WHERE c.motorista.id = :motoristaId " +
        "AND c.status IN :statusList " +
        "ORDER BY c.dataHoraPartida DESC")
    Page<Carona> findHistoricoComoMotorista(@Param("motoristaId") Long motoristaId,
                                        @Param("statusList") List<StatusCarona> statusList,
                                        Pageable pageable);

    interface CaronaProximaProjection {
        Long getId();
        String getOrigemDescricao();
        BigDecimal getOrigemLatitude();
        BigDecimal getOrigemLongitude();
        String getDestinoDescricao();
        BigDecimal getDestinoLatitude();
        BigDecimal getDestinoLongitude();
        LocalDateTime getDataHoraPartida();
        Integer getVagasTotais();
        com.unicar.enums.StatusCarona getStatus();
        Double getDistanciaKm();
    }

    boolean existsByMotoristaIdAndDataHoraPartidaAndStatusIn(
            Long motoristaId,
            LocalDateTime dataHoraPartida,
            List<StatusCarona> statuses
    );

    boolean existsByVeiculoId(Long veiculoID);

    long countByMotoristaIdAndStatus(
            Long motoristaId,
            StatusCarona status
    );
}
