package com.unicar.repository;

import com.unicar.domain.Usuario;
import com.unicar.domain.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    List<Veiculo> findByUsuarioOrderByIdAsc(Usuario usuario);

    Optional<Veiculo> findByIdAndUsuario(Long id, Usuario usuario);

    boolean existsByPlacaIgnoreCase(String placa);

    boolean existsByPlacaIgnoreCaseAndIdNot(String placa, Long id);
}
