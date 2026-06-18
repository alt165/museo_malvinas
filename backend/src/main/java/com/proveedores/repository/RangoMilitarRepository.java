package com.proveedores.repository;

import com.proveedores.entity.Fuerza;
import com.proveedores.entity.RangoMilitar;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RangoMilitarRepository extends JpaRepository<RangoMilitar, Long> {

    List<RangoMilitar> findByFuerzaAndActivoTrueAndEliminadoFalseOrderByOrdenJerarquicoAsc(Fuerza fuerza);
}
