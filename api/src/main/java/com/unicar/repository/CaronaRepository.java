package com.unicar.repository;

import com.unicar.domain.Carona;
import com.unicar.enums.StatusCarona;

import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query(value = """
        SELECT c.* FROM carona c
        JOIN usuario u ON c.motorista_id = u.id
        WHERE c.status = 'CRIADA'
          AND c.data_hora_partida > CURRENT_TIMESTAMP
          AND c.vagas_totais > 0
          
          -- Proximidade da Origem (Fórmula de Haversine adaptada para metros)
          AND (:origLat IS NULL OR :origLon IS NULL OR (6371000 * acos(
                cos(radians(:origLat)) * cos(radians(c.origem_latitude)) * 
                cos(radians(c.origem_longitude) - radians(:origLon)) + 
                sin(radians(:origLat)) * sin(radians(c.origem_latitude))
              )) <= :raioMetros)
              
          -- Proximidade do Destino (Fórmula de Haversine adaptada para metros)
          AND (:destLat IS NULL OR :destLon IS NULL OR (6371000 * acos(
                cos(radians(:destLat)) * cos(radians(c.destino_latitude)) * 
                cos(radians(c.destino_longitude) - radians(:destLon)) + 
                sin(radians(:destLat)) * sin(radians(c.destino_latitude))
              )) <= :raioMetros)
              
          -- Filtros Opcionais do Motorista
          AND (:genero IS NULL OR u.genero = :genero)
          AND (:curso IS NULL OR u.curso = :curso)
        """,
            nativeQuery = true)
    List<Carona> buscarCaronasComFiltrosComplexos(
            @Param("origLat") Double origLat,
            @Param("origLon") Double origLon,
            @Param("destLat") Double destLat,
            @Param("destLon") Double destLon,
            @Param("raioMetros") Double raioMetros,
            @Param("genero") String genero,
            @Param("curso") String curso
    );
}
