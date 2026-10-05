package com.proveedores.repository;

import com.proveedores.entity.Veterano;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VeteranoRepository extends JpaRepository<Veterano, Long>, JpaSpecificationExecutor<Veterano> {

    List<Veterano> findByApellidoContainingIgnoreCaseAndEliminadoFalse(String apellido);
}
