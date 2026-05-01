package com.proveedores.repository;

import com.proveedores.entity.Auditoria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    List<Auditoria> findByEntidadAndEntidadIdOrderByFechaDesc(String entidad, Long entidadId);

    List<Auditoria> findByUsuarioIdOrderByFechaDesc(Long usuarioId);
}
