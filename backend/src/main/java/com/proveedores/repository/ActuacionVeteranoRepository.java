package com.proveedores.repository;

import com.proveedores.entity.ActuacionVeterano;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ActuacionVeteranoRepository extends JpaRepository<ActuacionVeterano, Long>, JpaSpecificationExecutor<ActuacionVeterano> {

    @Override
    @EntityGraph(attributePaths = {"veterano", "rangoMilitar", "unidadMilitar"})
    Page<ActuacionVeterano> findAll(Specification<ActuacionVeterano> specification, Pageable pageable);

    List<ActuacionVeterano> findByVeteranoIdAndEliminadoFalseOrderByFechaInicioAsc(Long veteranoId);
}
