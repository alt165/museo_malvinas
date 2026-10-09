package com.proveedores.repository;

import com.proveedores.entity.EstadoExhibicion;
import com.proveedores.entity.Exhibicion;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExhibicionRepository extends JpaRepository<Exhibicion, Long>, JpaSpecificationExecutor<Exhibicion> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Exhibicion e where e.id = :id")
    Optional<Exhibicion> lockByIdForUpdate(@Param("id") Long id);

    List<Exhibicion> findByEstadoAndEliminadoFalse(EstadoExhibicion estado);

    Page<Exhibicion> findByEstadoAndEliminadoFalse(EstadoExhibicion estado, Pageable pageable);

    List<Exhibicion> findByEstadoAndEliminadoFalseAndFechaInicioLessThanEqual(EstadoExhibicion estado, LocalDate fechaInicio);

    @Query("""
            select e
            from Exhibicion e
            where e.eliminado = false
              and e.estado = com.proveedores.entity.EstadoExhibicion.PLANIFICADA
              and e.fechaInicio between :desde and :hasta
            order by e.fechaInicio asc, e.nombre asc
            """)
    List<Exhibicion> buscarProximasAIniciar(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

}
