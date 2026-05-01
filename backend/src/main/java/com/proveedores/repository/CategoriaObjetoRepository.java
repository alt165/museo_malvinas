package com.proveedores.repository;

import com.proveedores.entity.CategoriaObjeto;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaObjetoRepository extends JpaRepository<CategoriaObjeto, Long> {

    Optional<CategoriaObjeto> findByNombreAndEliminadoFalse(String nombre);

    boolean existsByNombre(String nombre);
}
