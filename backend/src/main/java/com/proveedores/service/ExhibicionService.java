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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExhibicionService {

    private final ExhibicionRepository exhibicionRepository;
    private final ExhibicionObjetoRepository exhibicionObjetoRepository;

    public ExhibicionService(ExhibicionRepository exhibicionRepository, ExhibicionObjetoRepository exhibicionObjetoRepository) {
        this.exhibicionRepository = exhibicionRepository;
        this.exhibicionObjetoRepository = exhibicionObjetoRepository;
    }

    @Transactional
    public ExhibicionResponseDTO crear(ExhibicionRequestDTO dto) {
        return ExhibicionMapper.toResponse(exhibicionRepository.save(ExhibicionMapper.toEntity(dto)));
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
        return ExhibicionMapper.toResponse(exhibicionRepository.save(entity));
    }

    @Transactional
    public ExhibicionResponseDTO finalizar(Long id) {
        Exhibicion entity = buscarActivo(id);
        List<ExhibicionObjeto> objetos = exhibicionObjetoRepository.findByExhibicionIdAndEliminadoFalse(id);
        boolean hayPendientes = objetos.stream().anyMatch(objeto -> !Boolean.TRUE.equals(objeto.getDevolucionVerificada()) || objeto.getEstado() != EstadoExhibicionObjeto.DEVUELTO);
        if (hayPendientes) {
            throw new BusinessException("No se puede finalizar la exhibicion con objetos pendientes de devolucion");
        }
        entity.setEstado(EstadoExhibicion.FINALIZADA);
        return ExhibicionMapper.toResponse(exhibicionRepository.save(entity));
    }

    @Transactional
    public void bajaLogica(Long id) {
        Exhibicion entity = buscarActivo(id);
        if (entity.getEstado() == EstadoExhibicion.ACTIVA) {
            throw new BusinessException("No se puede dar de baja una exhibicion activa");
        }
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(LocalDateTime.now());
        exhibicionRepository.save(entity);
    }

    private Exhibicion buscarActivo(Long id) {
        Exhibicion entity = exhibicionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Exhibicion no encontrada"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Exhibicion no encontrada");
        }
        return entity;
    }
}
