package com.proveedores.service;

import com.proveedores.dto.VeteranoRequestDTO;
import com.proveedores.dto.VeteranoResponseDTO;
import com.proveedores.entity.Veterano;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.VeteranoMapper;
import com.proveedores.repository.VeteranoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VeteranoService {

    private final VeteranoRepository veteranoRepository;

    public VeteranoService(VeteranoRepository veteranoRepository) {
        this.veteranoRepository = veteranoRepository;
    }

    @Transactional
    public VeteranoResponseDTO crear(VeteranoRequestDTO dto) {
        return VeteranoMapper.toResponse(veteranoRepository.save(VeteranoMapper.toEntity(dto)));
    }

    @Transactional(readOnly = true)
    public VeteranoResponseDTO obtenerPorId(Long id) {
        return VeteranoMapper.toResponse(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<VeteranoResponseDTO> listar() {
        return veteranoRepository.findAll().stream().filter(e -> !e.getEliminado()).map(VeteranoMapper::toResponse).toList();
    }

    @Transactional
    public VeteranoResponseDTO actualizar(Long id, VeteranoRequestDTO dto) {
        Veterano entity = buscarActivo(id);
        entity.setNombre(dto.nombre());
        entity.setApellido(dto.apellido());
        entity.setFuerza(dto.fuerza());
        entity.setFechaNacimiento(dto.fechaNacimiento());
        entity.setFechaFallecimiento(dto.fechaFallecimiento());
        entity.setHistoria(dto.historia());
        return VeteranoMapper.toResponse(veteranoRepository.save(entity));
    }

    @Transactional
    public void bajaLogica(Long id) {
        Veterano entity = buscarActivo(id);
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(LocalDateTime.now());
        veteranoRepository.save(entity);
    }

    public Veterano buscarActivo(Long id) {
        Veterano entity = veteranoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Veterano no encontrado"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Veterano no encontrado");
        }
        return entity;
    }
}
