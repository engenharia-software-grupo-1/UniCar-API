package com.unicar.repository;

import com.unicar.domain.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    List<Avaliacao> findByAvaliadoId(Long avaliadoId);

    boolean existsByCaronaIdAndAvaliadorIdAndAvaliadoId(Long caronaId, Long avaliadorId, Long avaliadoId);

    @Query("""
            SELECT AVG(a.nota)
            FROM Avaliacao a
            WHERE a.avaliado.id = :usuarioId
            """)
    Double calcularMedia(Long usuarioId);

    Long countByAvaliadoId(Long usuarioId);

}