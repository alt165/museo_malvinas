package com.proveedores.repository;

import com.proveedores.entity.RelacionObjeto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RelacionObjetoRepository extends JpaRepository<RelacionObjeto, Long> {

    List<RelacionObjeto> findByObjetoOrigenIdAndEliminadoFalse(Long objetoOrigenId);

    List<RelacionObjeto> findByObjetoDestinoIdAndEliminadoFalse(Long objetoDestinoId);

    Optional<RelacionObjeto> findByObjetoOrigenIdAndObjetoDestinoIdAndTipoRelacionAndEliminadoFalse(
            Long objetoOrigenId,
            Long objetoDestinoId,
            String tipoRelacion
    );

    @Query("""
            select ro
            from RelacionObjeto ro
            where ro.eliminado = false
              and (ro.objetoOrigen.id = :objetoMuseoId or ro.objetoDestino.id = :objetoMuseoId)
            """)
    List<RelacionObjeto> findAllByObjetoMuseoId(@Param("objetoMuseoId") Long objetoMuseoId);
}
