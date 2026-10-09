package com.proveedores.repository;

import com.proveedores.entity.ObjetoCategoria;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObjetoCategoriaRepository extends JpaRepository<ObjetoCategoria, Long> {

    List<ObjetoCategoria> findByObjetoMuseoIdAndEliminadoFalse(Long objetoMuseoId);

    List<ObjetoCategoria> findByCategoriaObjetoIdAndEliminadoFalse(Long categoriaObjetoId);

    boolean existsByObjetoMuseoIdAndCategoriaObjetoIdAndEliminadoFalse(Long objetoMuseoId, Long categoriaObjetoId);

    Optional<ObjetoCategoria> findByObjetoMuseoIdAndCategoriaObjetoId(Long objetoMuseoId, Long categoriaObjetoId);
}
