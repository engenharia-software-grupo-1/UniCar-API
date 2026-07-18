package com.unicar.repository;

import com.unicar.domain.InteresseTrajeto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface InteresseTrajetoRepository extends JpaRepository<InteresseTrajeto, Long> {

    List<InteresseTrajeto> findByUsuarioId(Long usuarioId);

    Optional<InteresseTrajeto> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByUsuarioIdAndOrigemLatitudeAndOrigemLongitudeAndDestinoLatitudeAndDestinoLongitude(
            Long usuarioId,
            BigDecimal origemLatitude,
            BigDecimal origemLongitude,
            BigDecimal destinoLatitude,
            BigDecimal destinoLongitude
    );
}
