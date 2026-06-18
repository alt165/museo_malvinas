package com.proveedores.mapper;

import com.proveedores.dto.ObjetoVeteranoRequestDTO;
import com.proveedores.dto.ObjetoVeteranoResponseDTO;
import com.proveedores.entity.ObjetoVeterano;

public final class ObjetoVeteranoMapper {
    private ObjetoVeteranoMapper() {
    }

    public static ObjetoVeterano toEntity(ObjetoVeteranoRequestDTO dto) {
        ObjetoVeterano entity = new ObjetoVeterano();
        entity.setObjetoMuseo(MapperReferences.objetoMuseo(dto.objetoMuseoId()));
        entity.setVeterano(MapperReferences.veterano(dto.veteranoId()));
        entity.setTipoRelacion(dto.tipoRelacion());
        entity.setDescripcion(dto.descripcion());
        return entity;
    }

    public static ObjetoVeteranoResponseDTO toResponse(ObjetoVeterano entity) {
        return new ObjetoVeteranoResponseDTO(entity.getId(), entity.getObjetoMuseo().getId(), entity.getObjetoMuseo().getDenominacionObjeto(), entity.getVeterano().getId(), entity.getVeterano().getNombre() + " " + entity.getVeterano().getApellido(), entity.getTipoRelacion(), entity.getDescripcion());
    }
}
