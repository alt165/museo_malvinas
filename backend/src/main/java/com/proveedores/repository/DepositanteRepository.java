package com.proveedores.repository;

import com.proveedores.entity.Depositante;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepositanteRepository extends JpaRepository<Depositante, Long>, JpaSpecificationExecutor<Depositante> {

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

    @Query(value = "select * from depositantes d where regexp_replace(d.dni, '[^0-9]', '', 'g') = :identificacion order by d.id limit 1", nativeQuery = true)
    Optional<Depositante> findPrimeroPorDniNormalizado(@Param("identificacion") String identificacion);

    @Query(value = "select * from depositantes d where regexp_replace(d.cuit, '[^0-9]', '', 'g') = :identificacion order by d.id limit 1", nativeQuery = true)
    Optional<Depositante> findPrimeroPorCuitNormalizado(@Param("identificacion") String identificacion);
}
