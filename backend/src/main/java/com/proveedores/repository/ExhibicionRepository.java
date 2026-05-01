package com.proveedores.repository;

import com.proveedores.entity.EstadoExhibicion;
import com.proveedores.entity.Exhibicion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExhibicionRepository extends JpaRepository<Exhibicion, Long> {

    List<Exhibicion> findByEstadoAndEliminadoFalse(EstadoExhibicion estado);
}
