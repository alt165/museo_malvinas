package com.proveedores.repository;

import com.proveedores.entity.Inventario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    Optional<Inventario> findByObjetoMuseoIdAndEliminadoFalse(Long objetoMuseoId);

    Optional<Inventario> findByObjetoMuseoId(Long objetoMuseoId);

    List<Inventario> findByUbicacionIdAndEliminadoFalse(Long ubicacionId);
}
