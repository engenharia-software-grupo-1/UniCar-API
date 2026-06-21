package com.unicar.repository;

import org.springframework.stereotype.Repository;

import com.unicar.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}
