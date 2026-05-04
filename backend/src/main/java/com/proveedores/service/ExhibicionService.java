package com.proveedores.service;

import com.proveedores.dto.ExhibicionRequestDTO;
import com.proveedores.dto.ExhibicionResponseDTO;
import com.proveedores.entity.EstadoExhibicion;
import com.proveedores.entity.EstadoExhibicionObjeto;
import com.proveedores.entity.Exhibicion;
import com.proveedores.entity.ExhibicionObjeto;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.ExhibicionMapper;
import com.proveedores.repository.ExhibicionObjetoRepository;
import com.proveedores.repository.ExhibicionRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExhibicionService {

    private static final Logger log = LoggerFactory.getLogger(ExhibicionService.class);

    private final ExhibicionRepository exhibicionRepository;
    private final ExhibicionObjetoRepository exhibicionObjetoRepository;

    public ExhibicionService(ExhibicionRepository exhibicionRepository, ExhibicionObjetoRepository exhibicionObjetoRepository) {
        this.exhibicionRepository = exhibicionRepository;
        this.exhibicionObjetoRepository = exhibicionObjetoRepository;
    }

    @Transactional
    public ExhibicionResponseDTO crear(ExhibicionRequestDTO dto) {
        Exhibicion saved = exhibicionRepository.save(ExhibicionMapper.toEntity(dto));
        log.info("event=exhibicion.created exhibicionId={} estado={} tipo={}", saved.getId(), saved.getEstado(), saved.getTipo());
        return ExhibicionMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ExhibicionResponseDTO obtenerPorId(Long id) {
        return ExhibicionMapper.toResponse(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<ExhibicionResponseDTO> listar() {
        return exhibicionRepository.findAll().stream().filter(e -> !e.getEliminado()).map(ExhibicionMapper::toResponse).toList();
    }

    @Transactional
    public ExhibicionResponseDTO actualizar(Long id, ExhibicionRequestDTO dto) {
        Exhibicion entity = buscarActivo(id);
        entity.setNombre(dto.nombre());
        entity.setDescripcion(dto.descripcion());
        entity.setTipo(dto.tipo());
        entity.setFechaInicio(dto.fechaInicio());
        entity.setFechaFin(dto.fechaFin());
        entity.setEstado(dto.estado());
        Exhibicion saved = exhibicionRepository.save(entity);
        log.info("event=exhibicion.updated exhibicionId={} estado={}", saved.getId(), saved.getEstado());
        return ExhibicionMapper.toResponse(saved);
    }

    @Transactional
    public ExhibicionResponseDTO finalizar(Long id) {
        Exhibicion entity = buscarActivo(id);
        List<ExhibicionObjeto> objetos = exhibicionObjetoRepository.findByExhibicionIdAndEliminadoFalse(id);
        boolean hayPendientes = objetos.stream().anyMatch(objeto -> !Boolean.TRUE.equals(objeto.getDevolucionVerificada()) || objeto.getEstado() != EstadoExhibicionObjeto.DEVUELTO);
        if (hayPendientes) {
            log.warn("event=exhibicion.business_error reason=objetos_pendientes_devolucion exhibicionId={} objetosAsociados={}", id, objetos.size());
            throw new BusinessException("No se puede finalizar la exhibicion con objetos pendientes de devolucion");
        }
        entity.setEstado(EstadoExhibicion.FINALIZADA);
        Exhibicion saved = exhibicionRepository.save(entity);
        log.info("event=exhibicion.finalized exhibicionId={} objetosAsociados={}", saved.getId(), objetos.size());
        return ExhibicionMapper.toResponse(saved);
    }

    @Transactional
    public void bajaLogica(Long id) {
        Exhibicion entity = buscarActivo(id);
        if (entity.getEstado() == EstadoExhibicion.ACTIVA) {
            log.warn("event=exhibicion.business_error reason=baja_exhibicion_activa exhibicionId={}", id);
            throw new BusinessException("No se puede dar de baja una exhibicion activa");
        }
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(LocalDateTime.now());
        exhibicionRepository.save(entity);
        log.info("event=exhibicion.deleted exhibicionId={}", entity.getId());
    }

    private Exhibicion buscarActivo(Long id) {
        Exhibicion entity = exhibicionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Exhibicion no encontrada"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Exhibicion no encontrada");
        }
        return entity;
    }
}
