package com.proveedores.repository;

import com.proveedores.entity.Ubicacion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UbicacionRepository extends JpaRepository<Ubicacion, Long> {

    Optional<Ubicacion> findByNombreAndEliminadoFalse(String nombre);

    Optional<Ubicacion> findByNombre(String nombre);

    boolean existsByNombre(String nombre);
}
