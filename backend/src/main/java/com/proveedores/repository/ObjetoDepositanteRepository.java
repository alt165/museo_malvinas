package com.proveedores.repository;

import com.proveedores.entity.ObjetoDepositante;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObjetoDepositanteRepository extends JpaRepository<ObjetoDepositante, Long> {

    List<ObjetoDepositante> findByObjetoMuseoIdAndEliminadoFalse(Long objetoMuseoId);

    Optional<ObjetoDepositante> findFirstByObjetoMuseoIdAndEliminadoFalseOrderByIdAsc(Long objetoMuseoId);

    List<ObjetoDepositante> findByDepositanteIdAndEliminadoFalse(Long depositanteId);

    boolean existsByObjetoMuseoIdAndDepositanteIdAndEliminadoFalse(Long objetoMuseoId, Long depositanteId);
}
