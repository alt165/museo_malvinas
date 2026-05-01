package com.proveedores.mapper;

import com.proveedores.dto.CategoriaObjetoRequestDTO;
import com.proveedores.dto.CategoriaObjetoResponseDTO;
import com.proveedores.entity.CategoriaObjeto;

public final class CategoriaObjetoMapper {
    private CategoriaObjetoMapper() {
    }

    public static CategoriaObjeto toEntity(CategoriaObjetoRequestDTO dto) {
        CategoriaObjeto entity = new CategoriaObjeto();
        entity.setNombre(dto.nombre());
        entity.setDescripcion(dto.descripcion());
        return entity;
    }

    public static CategoriaObjetoResponseDTO toResponse(CategoriaObjeto entity) {
        return new CategoriaObjetoResponseDTO(entity.getId(), entity.getNombre(), entity.getDescripcion());
    }
}
