package com.proveedores.service;

import com.proveedores.dto.UbicacionRequestDTO;
import com.proveedores.dto.UbicacionResponseDTO;
import com.proveedores.entity.Ubicacion;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.UbicacionMapper;
import com.proveedores.repository.UbicacionRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UbicacionService {

    private final UbicacionRepository ubicacionRepository;

    public UbicacionService(UbicacionRepository ubicacionRepository) {
        this.ubicacionRepository = ubicacionRepository;
    }

    @Transactional
    public UbicacionResponseDTO crear(UbicacionRequestDTO dto) {
        return UbicacionMapper.toResponse(ubicacionRepository.save(UbicacionMapper.toEntity(dto)));
    }

    @Transactional(readOnly = true)
    public UbicacionResponseDTO obtenerPorId(Long id) {
        return UbicacionMapper.toResponse(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<UbicacionResponseDTO> listar() {
        return ubicacionRepository.findAll().stream().filter(e -> !e.getEliminado()).map(UbicacionMapper::toResponse).toList();
    }

    @Transactional
    public UbicacionResponseDTO actualizar(Long id, UbicacionRequestDTO dto) {
        Ubicacion entity = buscarActivo(id);
        entity.setNombre(dto.nombre());
        entity.setTipo(dto.tipo());
        entity.setDescripcion(dto.descripcion());
        return UbicacionMapper.toResponse(ubicacionRepository.save(entity));
    }

    @Transactional
    public void bajaLogica(Long id) {
        Ubicacion entity = buscarActivo(id);
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(LocalDateTime.now());
        ubicacionRepository.save(entity);
    }

    private Ubicacion buscarActivo(Long id) {
        Ubicacion entity = ubicacionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ubicacion no encontrada"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Ubicacion no encontrada");
        }
        return entity;
    }
}
