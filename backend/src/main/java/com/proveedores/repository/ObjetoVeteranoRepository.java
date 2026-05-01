package com.proveedores.repository;

import com.proveedores.entity.ObjetoVeterano;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObjetoVeteranoRepository extends JpaRepository<ObjetoVeterano, Long> {

    List<ObjetoVeterano> findByObjetoMuseoIdAndEliminadoFalse(Long objetoMuseoId);

    List<ObjetoVeterano> findByVeteranoIdAndEliminadoFalse(Long veteranoId);

    boolean existsByObjetoMuseoIdAndVeteranoIdAndTipoRelacionAndEliminadoFalse(
            Long objetoMuseoId,
            Long veteranoId,
            String tipoRelacion
    );
}
