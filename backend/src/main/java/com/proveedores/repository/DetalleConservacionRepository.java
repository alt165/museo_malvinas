package com.proveedores.repository;

import com.proveedores.entity.DetalleConservacion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleConservacionRepository extends JpaRepository<DetalleConservacion, Long> {

    List<DetalleConservacion> findByActivoTrueAndEliminadoFalseOrderByNombreAsc();

    List<DetalleConservacion> findByCodigoInAndActivoTrueAndEliminadoFalse(Collection<String> codigos);

    Optional<DetalleConservacion> findByCodigo(String codigo);
}
