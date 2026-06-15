package com.proveedores.repository;

import com.proveedores.entity.EstadoExhibicion;
import com.proveedores.entity.Exhibicion;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExhibicionRepository extends JpaRepository<Exhibicion, Long> {

    List<Exhibicion> findByEstadoAndEliminadoFalse(EstadoExhibicion estado);

    Page<Exhibicion> findByEstadoAndEliminadoFalse(EstadoExhibicion estado, Pageable pageable);

    @Query("""
            select e
            from Exhibicion e
            where e.eliminado = false
              and e.estado = com.proveedores.entity.EstadoExhibicion.FINALIZADA
              and (
                lower(e.nombre) like lower(concat('%', :texto, '%'))
                or lower(coalesce(e.descripcion, '')) like lower(concat('%', :texto, '%'))
              )
            """)
    Page<Exhibicion> buscarFinalizadasPorTexto(@Param("texto") String texto, Pageable pageable);
}
