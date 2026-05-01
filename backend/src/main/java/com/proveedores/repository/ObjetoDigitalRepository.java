package com.proveedores.repository;

import com.proveedores.entity.ObjetoDigital;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ObjetoDigitalRepository extends JpaRepository<ObjetoDigital, Long> {

    @Query("""
            select od
            from ObjetoDigital od
            where od.id = :objetoMuseoId
              and od.eliminado = false
            """)
    Optional<ObjetoDigital> findByObjetoMuseoId(@Param("objetoMuseoId") Long objetoMuseoId);
}
