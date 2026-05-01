package com.proveedores.repository;

import com.proveedores.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByKeycloakIdAndEliminadoFalse(String keycloakId);

    Optional<Usuario> findByEmailAndEliminadoFalse(String email);
}
