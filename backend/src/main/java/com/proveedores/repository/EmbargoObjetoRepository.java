package com.proveedores.repository;

import com.proveedores.entity.EmbargoObjeto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmbargoObjetoRepository extends JpaRepository<EmbargoObjeto, Long>, JpaSpecificationExecutor<EmbargoObjeto> {

    boolean existsByObjetoMuseoIdAndFechaFinalizacionIsNullAndEliminadoFalse(Long objetoMuseoId);

    Optional<EmbargoObjeto> findByObjetoMuseoIdAndFechaFinalizacionIsNullAndEliminadoFalse(Long objetoMuseoId);

    List<EmbargoObjeto> findByFechaFinalizacionIsNullAndEliminadoFalseOrderByFechaInicioDescIdDesc();

    List<EmbargoObjeto> findByEliminadoFalseOrderByFechaFinalizacionAscFechaInicioDescIdDesc();
}
