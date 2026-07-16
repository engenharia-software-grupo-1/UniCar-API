package com.unicar.repository;

import com.unicar.domain.BloqueioUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BloqueioUsuarioRepository extends JpaRepository<BloqueioUsuario, Long> {

    boolean existsByUsuarioIdAndUsuarioBloqueadoId(Long usuarioId, Long usuarioBloqueadoId);

    Optional<BloqueioUsuario> findByUsuarioIdAndUsuarioBloqueadoId(Long usuarioId, Long usuarioBloqueadoId);

    List<BloqueioUsuario> findAllByUsuarioId(Long usuarioId);
}