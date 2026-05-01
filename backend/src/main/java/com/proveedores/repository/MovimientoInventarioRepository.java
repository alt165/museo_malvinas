package com.proveedores.repository;

import com.proveedores.entity.MovimientoInventario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    List<MovimientoInventario> findByObjetoMuseoIdAndEliminadoFalseOrderByFechaDesc(Long objetoMuseoId);
}
