package com.proveedores.repository;

import com.proveedores.entity.VeteranoImagen;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VeteranoImagenRepository extends JpaRepository<VeteranoImagen, Long> {

    List<VeteranoImagen> findByVeteranoIdAndEliminadoFalseOrderByOrdenAscFechaCargaAscIdAsc(Long veteranoId);

    Optional<VeteranoImagen> findByIdAndVeteranoIdAndEliminadoFalse(Long id, Long veteranoId);

    long countByVeteranoIdAndEliminadoFalse(Long veteranoId);
}
