package com.proveedores.mapper;

import com.proveedores.dto.ActuacionVeteranoRequestDTO;
import com.proveedores.dto.ActuacionVeteranoResponseDTO;
import com.proveedores.entity.ActuacionVeterano;
import com.proveedores.entity.RangoMilitar;
import com.proveedores.entity.UnidadMilitar;

public final class ActuacionVeteranoMapper {
    private ActuacionVeteranoMapper() {
    }

    public static ActuacionVeterano toEntity(ActuacionVeteranoRequestDTO dto) {
        ActuacionVeterano entity = new ActuacionVeterano();
        entity.setVeterano(MapperReferences.veterano(dto.veteranoId()));
        entity.setRango(dto.rango());
        entity.setUnidad(dto.unidad());
        entity.setRol(dto.rol());
        entity.setFechaInicio(dto.fechaInicio());
        entity.setFechaFin(dto.fechaFin());
        entity.setDescripcion(dto.descripcion());
        return entity;
    }

    public static ActuacionVeteranoResponseDTO toResponse(ActuacionVeterano entity) {
        RangoMilitar rangoMilitar = entity.getRangoMilitar();
        UnidadMilitar unidadMilitar = entity.getUnidadMilitar();
        String rangoNombre = rangoMilitar != null ? rangoMilitar.getNombre() : entity.getRango();
        String unidadNombre = unidadMilitar != null ? unidadMilitar.getNombre() : entity.getUnidad();
        return new ActuacionVeteranoResponseDTO(
                entity.getId(),
                entity.getVeterano().getId(),
                entity.getVeterano().getNombre() + " " + entity.getVeterano().getApellido(),
                entity.getRango(),
                entity.getUnidad(),
                rangoMilitar != null ? rangoMilitar.getId() : null,
                rangoNombre,
                unidadMilitar != null ? unidadMilitar.getId() : null,
                unidadNombre,
                unidadMilitar != null ? unidadMilitar.getSigla() : null,
                entity.getRol(),
                entity.getFechaInicio(),
                entity.getFechaFin(),
                entity.getDescripcion()
        );
    }
}
