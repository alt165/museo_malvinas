package com.proveedores.repository;

import com.proveedores.entity.FotoObjetoMuseo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FotoObjetoMuseoRepository extends JpaRepository<FotoObjetoMuseo, Long> {

    List<FotoObjetoMuseo> findByObjetoMuseoIdAndEliminadoFalse(Long objetoMuseoId);

    Optional<FotoObjetoMuseo> findByIdAndObjetoMuseoIdAndEliminadoFalse(Long id, Long objetoMuseoId);
}
