package com.proveedores.repository;

import com.proveedores.entity.Veterano;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VeteranoRepository extends JpaRepository<Veterano, Long> {

    List<Veterano> findByApellidoContainingIgnoreCaseAndEliminadoFalse(String apellido);
}
