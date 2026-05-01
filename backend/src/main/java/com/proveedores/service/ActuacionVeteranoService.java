package com.proveedores.service;

import com.proveedores.dto.ActuacionVeteranoRequestDTO;
import com.proveedores.dto.ActuacionVeteranoResponseDTO;
import com.proveedores.entity.ActuacionVeterano;
import com.proveedores.entity.Veterano;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.ActuacionVeteranoMapper;
import com.proveedores.repository.ActuacionVeteranoRepository;
import com.proveedores.repository.VeteranoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActuacionVeteranoService {

    private final ActuacionVeteranoRepository actuacionVeteranoRepository;
    private final VeteranoRepository veteranoRepository;

    public ActuacionVeteranoService(ActuacionVeteranoRepository actuacionVeteranoRepository, VeteranoRepository veteranoRepository) {
        this.actuacionVeteranoRepository = actuacionVeteranoRepository;
        this.veteranoRepository = veteranoRepository;
    }

    @Transactional
    public ActuacionVeteranoResponseDTO crear(ActuacionVeteranoRequestDTO dto) {
        ActuacionVeterano entity = ActuacionVeteranoMapper.toEntity(dto);
        entity.setVeterano(buscarVeterano(dto.veteranoId()));
        return ActuacionVeteranoMapper.toResponse(actuacionVeteranoRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public ActuacionVeteranoResponseDTO obtenerPorId(Long id) {
        return ActuacionVeteranoMapper.toResponse(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<ActuacionVeteranoResponseDTO> listar() {
        return actuacionVeteranoRepository.findAll().stream().filter(e -> !e.getEliminado()).map(ActuacionVeteranoMapper::toResponse).toList();
    }

    @Transactional
    public ActuacionVeteranoResponseDTO actualizar(Long id, ActuacionVeteranoRequestDTO dto) {
        ActuacionVeterano entity = buscarActivo(id);
        entity.setVeterano(buscarVeterano(dto.veteranoId()));
        entity.setRango(dto.rango());
        entity.setUnidad(dto.unidad());
        entity.setRol(dto.rol());
        entity.setFechaInicio(dto.fechaInicio());
        entity.setFechaFin(dto.fechaFin());
        entity.setDescripcion(dto.descripcion());
        return ActuacionVeteranoMapper.toResponse(actuacionVeteranoRepository.save(entity));
    }

    @Transactional
    public void bajaLogica(Long id) {
        ActuacionVeterano entity = buscarActivo(id);
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(LocalDateTime.now());
        actuacionVeteranoRepository.save(entity);
    }

    private ActuacionVeterano buscarActivo(Long id) {
        ActuacionVeterano entity = actuacionVeteranoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Actuacion de veterano no encontrada"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Actuacion de veterano no encontrada");
        }
        return entity;
    }

    private Veterano buscarVeterano(Long id) {
        Veterano entity = veteranoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Veterano no encontrado"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Veterano no encontrado");
        }
        return entity;
    }
}
