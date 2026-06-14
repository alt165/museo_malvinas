package com.proveedores.repository;

import com.proveedores.entity.VeteranoVideo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VeteranoVideoRepository extends JpaRepository<VeteranoVideo, Long> {

    List<VeteranoVideo> findByVeteranoIdAndEliminadoFalseOrderByOrdenAscIdAsc(Long veteranoId);

    Optional<VeteranoVideo> findByIdAndVeteranoIdAndEliminadoFalse(Long id, Long veteranoId);

    long countByVeteranoIdAndEliminadoFalse(Long veteranoId);
}
