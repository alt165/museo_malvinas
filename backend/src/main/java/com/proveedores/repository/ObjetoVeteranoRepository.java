package com.proveedores.repository;

import com.proveedores.entity.ObjetoVeterano;
import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObjetoVeteranoRepository extends JpaRepository<ObjetoVeterano, Long> {

    List<ObjetoVeterano> findByObjetoMuseoIdAndEliminadoFalse(Long objetoMuseoId);

    List<ObjetoVeterano> findByVeteranoIdAndEliminadoFalse(Long veteranoId);

    @Query("""
            select ov from ObjetoVeterano ov
            join fetch ov.objetoMuseo objeto
            join fetch ov.veterano veterano
            where ov.eliminado = false and objeto.id in :objetoIds
            """)
    List<ObjetoVeterano> findAllByObjetoMuseoIds(@Param("objetoIds") Collection<Long> objetoIds);

    @Query("""
            select ov from ObjetoVeterano ov
            join fetch ov.objetoMuseo objeto
            join fetch ov.veterano veterano
            where ov.eliminado = false and veterano.id in :veteranoIds
            """)
    List<ObjetoVeterano> findAllByVeteranoIds(@Param("veteranoIds") Collection<Long> veteranoIds);

    boolean existsByObjetoMuseoIdAndVeteranoIdAndTipoRelacionAndEliminadoFalse(
            Long objetoMuseoId,
            Long veteranoId,
            String tipoRelacion
    );
}
