package com.proveedores.repository;

import com.proveedores.entity.EstadoExhibicion;
import com.proveedores.entity.ExhibicionObjeto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExhibicionObjetoRepository extends JpaRepository<ExhibicionObjeto, Long> {

    List<ExhibicionObjeto> findByExhibicionIdAndEliminadoFalse(Long exhibicionId);

    List<ExhibicionObjeto> findByObjetoMuseoIdAndEliminadoFalse(Long objetoMuseoId);

    @Query("""
            select count(eo) > 0
            from ExhibicionObjeto eo
            where eo.objetoMuseo.id = :objetoMuseoId
              and eo.eliminado = false
              and eo.exhibicion.eliminado = false
              and eo.exhibicion.estado = :estado
            """)
    boolean existsByObjetoMuseoIdInExhibicionWithEstado(
            @Param("objetoMuseoId") Long objetoMuseoId,
            @Param("estado") EstadoExhibicion estado
    );
}
