package com.proveedores.repository;

import com.proveedores.entity.Depositante;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepositanteRepository extends JpaRepository<Depositante, Long> {

    List<Depositante> findByNombreContainingIgnoreCaseAndEliminadoFalse(String nombre);

    @Query("""
            select d
            from Depositante d
            where d.eliminado = false
              and (
                replace(replace(replace(d.dni, '.', ''), '-', ''), ' ', '') = :identificacion
                or replace(replace(replace(d.cuit, '.', ''), '-', ''), ' ', '') = :identificacion
              )
            """)
    Optional<Depositante> findActivoByIdentificacionNormalizada(@Param("identificacion") String identificacion);
}
