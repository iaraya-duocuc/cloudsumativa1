package com.duoc.cloudsumativa1.repository;

import com.duoc.cloudsumativa1.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByCorreoIgnoreCase(String correo);
}
