package com.proveedores.repository;

import com.proveedores.entity.ObjetoCategoria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObjetoCategoriaRepository extends JpaRepository<ObjetoCategoria, Long> {

    List<ObjetoCategoria> findByObjetoMuseoIdAndEliminadoFalse(Long objetoMuseoId);

    List<ObjetoCategoria> findByCategoriaObjetoIdAndEliminadoFalse(Long categoriaObjetoId);

    boolean existsByObjetoMuseoIdAndCategoriaObjetoIdAndEliminadoFalse(Long objetoMuseoId, Long categoriaObjetoId);
}
