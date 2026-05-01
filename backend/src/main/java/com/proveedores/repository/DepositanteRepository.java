package com.proveedores.repository;

import com.proveedores.entity.Depositante;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositanteRepository extends JpaRepository<Depositante, Long> {

    List<Depositante> findByNombreContainingIgnoreCaseAndEliminadoFalse(String nombre);
}
