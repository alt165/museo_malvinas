package com.proveedores.repository;

import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.OrigenCargaObjeto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ObjetoMuseoRepository extends JpaRepository<ObjetoMuseo, Long>, JpaSpecificationExecutor<ObjetoMuseo> {

    Optional<ObjetoMuseo> findByNumeroInventario(String numeroInventario);

    boolean existsByNumeroInventario(String numeroInventario);

    org.springframework.data.domain.Page<ObjetoMuseo> findByOrigenCargaAndDatosCompletosFalseAndEliminadoFalse(
            OrigenCargaObjeto origenCarga,
            org.springframework.data.domain.Pageable pageable
    );

    List<ObjetoMuseo> findByOrigenCargaAndDatosCompletosFalseAndEliminadoFalse(OrigenCargaObjeto origenCarga, Sort sort);

    List<ObjetoMuseo> findByColeccionObjetoIsNullAndEliminadoFalseOrderByNumeroInventarioAsc();

    List<ObjetoMuseo> findByColeccionObjetoIdAndEliminadoFalseOrderByNumeroInventarioAsc(Long coleccionId);

    long countByColeccionObjetoIdAndEliminadoFalse(Long coleccionId);

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

    @Query("""
            select o
            from ObjetoMuseo o
            where o.eliminado = false
              and o.activo = true
              and (
                :texto is null
                or lower(o.numeroInventario) like lower(concat('%', :texto, '%'))
                or lower(o.denominacionObjeto) like lower(concat('%', :texto, '%'))
                or lower(coalesce(o.descripcion, '')) like lower(concat('%', :texto, '%'))
                or lower(coalesce(o.descripcionTecnica, '')) like lower(concat('%', :texto, '%'))
              )
            """)
    Page<ObjetoMuseo> buscarParaDisponibilidadExhibicion(@Param("texto") String texto, Pageable pageable);
}
