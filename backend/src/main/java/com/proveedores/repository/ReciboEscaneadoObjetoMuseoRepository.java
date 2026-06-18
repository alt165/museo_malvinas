package com.proveedores.repository;

import com.proveedores.entity.ReciboEscaneadoObjetoMuseo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReciboEscaneadoObjetoMuseoRepository extends JpaRepository<ReciboEscaneadoObjetoMuseo, Long> {

    Optional<ReciboEscaneadoObjetoMuseo> findFirstByObjetoMuseoIdAndEliminadoFalseOrderByFechaCargaDesc(Long objetoMuseoId);

    Optional<ReciboEscaneadoObjetoMuseo> findByIdAndObjetoMuseoIdAndEliminadoFalse(Long id, Long objetoMuseoId);
}
