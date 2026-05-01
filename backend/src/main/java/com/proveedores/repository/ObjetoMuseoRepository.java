package com.proveedores.repository;

import com.proveedores.entity.ObjetoMuseo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ObjetoMuseoRepository extends JpaRepository<ObjetoMuseo, Long> {

    Optional<ObjetoMuseo> findByNumeroInventario(String numeroInventario);

    boolean existsByNumeroInventario(String numeroInventario);

    @Query("""
            select oc.objetoMuseo
            from ObjetoCategoria oc
            where oc.categoriaObjeto.id = :categoriaId
              and oc.eliminado = false
              and oc.objetoMuseo.eliminado = false
            """)
    List<ObjetoMuseo> findByCategoriaId(@Param("categoriaId") Long categoriaId);

    @Query("""
            select od.objetoMuseo
            from ObjetoDepositante od
            where od.depositante.id = :depositanteId
              and od.eliminado = false
              and od.objetoMuseo.eliminado = false
            """)
    List<ObjetoMuseo> findByDepositanteId(@Param("depositanteId") Long depositanteId);

    @Query("""
            select ov.objetoMuseo
            from ObjetoVeterano ov
            where ov.veterano.id = :veteranoId
              and ov.eliminado = false
              and ov.objetoMuseo.eliminado = false
            """)
    List<ObjetoMuseo> findByVeteranoId(@Param("veteranoId") Long veteranoId);
}
