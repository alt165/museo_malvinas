package com.proveedores.repository;

import com.proveedores.entity.ColeccionObjeto;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColeccionObjetoRepository extends JpaRepository<ColeccionObjeto, Long> {

    Optional<ColeccionObjeto> findByNombreIgnoreCaseAndEliminadoFalse(String nombre);
}
