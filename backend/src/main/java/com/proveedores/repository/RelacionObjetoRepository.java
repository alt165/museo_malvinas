package com.proveedores.repository;

import com.proveedores.entity.RelacionObjeto;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RelacionObjetoRepository extends JpaRepository<RelacionObjeto, Long> {

    @org.springframework.data.jpa.repository.Query("""
            select distinct r from RelacionObjeto r
            join fetch r.objetoOrigen origen
            join fetch r.objetoDestino destino
            where r.eliminado = false
              and (origen.id in :objetoIds or destino.id in :objetoIds)
            """)
    List<RelacionObjeto> findAllByObjetoMuseoIds(@org.springframework.data.repository.query.Param("objetoIds") Collection<Long> objetoIds);

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
            join fetch ro.objetoOrigen
            join fetch ro.objetoDestino
            where ro.eliminado = false
              and (ro.objetoOrigen.id = :objetoMuseoId or ro.objetoDestino.id = :objetoMuseoId)
            order by ro.fechaCreacion desc, ro.id desc
            """)
    List<RelacionObjeto> findAllByObjetoMuseoId(@Param("objetoMuseoId") Long objetoMuseoId);
}
