package com.proveedores.repository;

import com.proveedores.entity.ReciboIngresoObjeto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReciboIngresoObjetoRepository extends JpaRepository<ReciboIngresoObjeto, Long> {

    List<ReciboIngresoObjeto> findByObjetoMuseoIdAndEliminadoFalse(Long objetoMuseoId);
}
