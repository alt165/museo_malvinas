package com.proveedores.mapper;

import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.entity.ObjetoMuseo;

public final class ObjetoMuseoMapper {
    private ObjetoMuseoMapper() {
    }

    public static ObjetoMuseo toEntity(ObjetoMuseoRequestDTO dto) {
        ObjetoMuseo entity = new ObjetoMuseo();
        entity.setNumeroInventario(dto.numeroInventario());
        entity.setNombre(dto.nombre());
        entity.setTipoObjeto(dto.tipoObjeto());
        entity.setDescripcion(dto.descripcion());
        return entity;
    }

    public static ObjetoMuseoResponseDTO toResponse(ObjetoMuseo entity) {
        return new ObjetoMuseoResponseDTO(entity.getId(), entity.getNumeroInventario(), entity.getNombre(), entity.getTipoObjeto(), entity.getDescripcion());
    }
}
