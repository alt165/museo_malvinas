package com.proveedores.repository;

import com.proveedores.entity.ColeccionObjeto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ColeccionObjetoRepository extends JpaRepository<ColeccionObjeto, Long>, JpaSpecificationExecutor<ColeccionObjeto> {

    Optional<ColeccionObjeto> findByNombreIgnoreCaseAndEliminadoFalse(String nombre);

    List<ColeccionObjeto> findAllByNombreIgnoreCaseOrderByIdAsc(String nombre);

    @Query("""
            select coleccion.id, count(objeto.id)
            from ColeccionObjeto coleccion
            left join coleccion.objetos objeto on objeto.eliminado = false
            where coleccion.id in :coleccionIds
            group by coleccion.id
            """)
    List<Object[]> contarObjetosNoEliminadosPorColeccionIds(@Param("coleccionIds") List<Long> coleccionIds);
}
