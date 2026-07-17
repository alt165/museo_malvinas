package com.proveedores.repository;

import com.proveedores.entity.Fuerza;
import com.proveedores.entity.UnidadMilitar;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnidadMilitarRepository extends JpaRepository<UnidadMilitar, Long> {

    List<UnidadMilitar> findByActivoTrueAndEliminadoFalseOrderByFuerzaAscNombreAsc();

    @Query("""
            select u
            from UnidadMilitar u
            where u.fuerza = :fuerza
              and u.activo = true
              and u.eliminado = false
              and (
                :buscar is null
                or lower(u.nombre) like lower(concat('%', :buscar, '%'))
                or lower(coalesce(u.sigla, '')) like lower(concat('%', :buscar, '%'))
              )
            order by coalesce(u.sigla, ''), u.nombre
            """)
    List<UnidadMilitar> buscarActivasPorFuerza(@Param("fuerza") Fuerza fuerza, @Param("buscar") String buscar, Pageable pageable);
}
