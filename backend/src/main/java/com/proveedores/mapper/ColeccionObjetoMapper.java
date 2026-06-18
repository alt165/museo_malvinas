package com.proveedores.mapper;

import com.proveedores.dto.ColeccionObjetoRequestDTO;
import com.proveedores.dto.ColeccionObjetoResponseDTO;
import com.proveedores.entity.ColeccionObjeto;

public final class ColeccionObjetoMapper {
    private ColeccionObjetoMapper() {
    }

    public static ColeccionObjeto toEntity(ColeccionObjetoRequestDTO dto) {
        ColeccionObjeto entity = new ColeccionObjeto();
        entity.setNombre(dto.nombre());
        entity.setDescripcion(dto.descripcion());
        return entity;
    }

    public static ColeccionObjetoResponseDTO toResponse(ColeccionObjeto entity, long cantidadObjetos) {
        return new ColeccionObjetoResponseDTO(
                entity.getId(),
                entity.getNombre(),
                entity.getDescripcion(),
                entity.getActivo(),
                cantidadObjetos
        );
    }
}
