package com.proveedores.repository;

import com.proveedores.entity.ActuacionVeterano;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActuacionVeteranoRepository extends JpaRepository<ActuacionVeterano, Long> {

    List<ActuacionVeterano> findByVeteranoIdAndEliminadoFalseOrderByFechaInicioAsc(Long veteranoId);
}
