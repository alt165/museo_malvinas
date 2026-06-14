package com.proveedores.service;

import com.proveedores.dto.ActuacionVeteranoRequestDTO;
import com.proveedores.dto.ActuacionVeteranoResponseDTO;
import com.proveedores.entity.ActuacionVeterano;
import com.proveedores.entity.Fuerza;
import com.proveedores.entity.RangoMilitar;
import com.proveedores.entity.UnidadMilitar;
import com.proveedores.entity.Veterano;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.ActuacionVeteranoMapper;
import com.proveedores.repository.ActuacionVeteranoRepository;
import com.proveedores.repository.RangoMilitarRepository;
import com.proveedores.repository.UnidadMilitarRepository;
import com.proveedores.repository.VeteranoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActuacionVeteranoService {

    private final ActuacionVeteranoRepository actuacionVeteranoRepository;
    private final VeteranoRepository veteranoRepository;
    private final RangoMilitarRepository rangoMilitarRepository;
    private final UnidadMilitarRepository unidadMilitarRepository;

    public ActuacionVeteranoService(
            ActuacionVeteranoRepository actuacionVeteranoRepository,
            VeteranoRepository veteranoRepository,
            RangoMilitarRepository rangoMilitarRepository,
            UnidadMilitarRepository unidadMilitarRepository
    ) {
        this.actuacionVeteranoRepository = actuacionVeteranoRepository;
        this.veteranoRepository = veteranoRepository;
        this.rangoMilitarRepository = rangoMilitarRepository;
        this.unidadMilitarRepository = unidadMilitarRepository;
    }

    @Transactional
    public ActuacionVeteranoResponseDTO crear(ActuacionVeteranoRequestDTO dto) {
        ActuacionVeterano entity = ActuacionVeteranoMapper.toEntity(dto);
        Veterano veterano = buscarVeterano(dto.veteranoId());
        entity.setVeterano(veterano);
        aplicarCatalogos(entity, dto, veterano.getFuerza());
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
        Veterano veterano = buscarVeterano(dto.veteranoId());
        entity.setVeterano(veterano);
        entity.setRol(dto.rol());
        entity.setFechaInicio(dto.fechaInicio());
        entity.setFechaFin(dto.fechaFin());
        entity.setDescripcion(dto.descripcion());
        aplicarCatalogos(entity, dto, veterano.getFuerza());
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

    private void aplicarCatalogos(ActuacionVeterano entity, ActuacionVeteranoRequestDTO dto, Fuerza fuerza) {
        RangoMilitar rango = dto.rangoId() == null ? null : buscarRangoCompatible(dto.rangoId(), fuerza);
        UnidadMilitar unidad = dto.unidadId() == null ? null : buscarUnidadCompatible(dto.unidadId(), fuerza);

        entity.setRangoMilitar(rango);
        entity.setUnidadMilitar(unidad);
        entity.setRango(rango != null ? rango.getNombre() : dto.rango());
        entity.setUnidad(unidad != null ? unidad.getNombre() : dto.unidad());
    }

    private RangoMilitar buscarRangoCompatible(Long id, Fuerza fuerza) {
        RangoMilitar rango = rangoMilitarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rango militar no encontrado"));
        if (!Boolean.TRUE.equals(rango.getActivo()) || Boolean.TRUE.equals(rango.getEliminado())) {
            throw new ResourceNotFoundException("Rango militar no encontrado");
        }
        if (rango.getFuerza() != fuerza) {
            throw new BusinessException("El rango seleccionado no pertenece a la fuerza del veterano");
        }
        return rango;
    }

    private UnidadMilitar buscarUnidadCompatible(Long id, Fuerza fuerza) {
        UnidadMilitar unidad = unidadMilitarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad militar no encontrada"));
        if (!Boolean.TRUE.equals(unidad.getActivo()) || Boolean.TRUE.equals(unidad.getEliminado())) {
            throw new ResourceNotFoundException("Unidad militar no encontrada");
        }
        if (unidad.getFuerza() != fuerza) {
            throw new BusinessException("La unidad seleccionada no pertenece a la fuerza del veterano");
        }
        return unidad;
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
