package com.unicar.repository;

import com.unicar.domain.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    List<Avaliacao> findByAvaliadoId(Long avaliadoId);

    boolean existsByCaronaIdAndAvaliadorId(Long caronaId, Long avaliadorId);

    @Query("""
            SELECT AVG(a.nota)
            FROM Avaliacao a
            WHERE a.avaliado.id = :usuarioId
            """)
    Double calcularMedia(Long usuarioId);

    Long countByAvaliadoId(Long usuarioId);

    @Query("""
        SELECT a.avaliado.id AS usuarioId,
               AVG(a.nota) AS media,
               COUNT(a) AS quantidade
        FROM Avaliacao a
        WHERE a.avaliado.id IN :usuarioIds
        GROUP BY a.avaliado.id
        """)
    List<ReputacaoAgregadaProjection> calcularMediasPorUsuarios(@Param("usuarioIds") List<Long> usuarioIds);

    /**
     * Projeção usada para agregar média e quantidade de avaliações
     * de vários usuários em uma única query, evitando N chamadas ao banco.
     */
    interface ReputacaoAgregadaProjection {
        Long getUsuarioId();
        Double getMedia();
        Long getQuantidade();
    }

}