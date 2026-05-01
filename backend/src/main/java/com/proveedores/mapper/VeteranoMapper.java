package com.proveedores.mapper;

import com.proveedores.dto.VeteranoRequestDTO;
import com.proveedores.dto.VeteranoResponseDTO;
import com.proveedores.entity.Veterano;

public final class VeteranoMapper {
    private VeteranoMapper() {
    }

    public static Veterano toEntity(VeteranoRequestDTO dto) {
        Veterano entity = new Veterano();
        entity.setNombre(dto.nombre());
        entity.setApellido(dto.apellido());
        entity.setFuerza(dto.fuerza());
        entity.setFechaNacimiento(dto.fechaNacimiento());
        entity.setFechaFallecimiento(dto.fechaFallecimiento());
        entity.setHistoria(dto.historia());
        return entity;
    }

    public static VeteranoResponseDTO toResponse(Veterano entity) {
        String nombreCompleto = entity.getNombre() + " " + entity.getApellido();
        return new VeteranoResponseDTO(entity.getId(), entity.getNombre(), entity.getApellido(), nombreCompleto, entity.getFuerza(), entity.getFechaNacimiento(), entity.getFechaFallecimiento(), entity.getHistoria());
    }
}
