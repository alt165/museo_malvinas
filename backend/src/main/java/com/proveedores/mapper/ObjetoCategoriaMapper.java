package com.proveedores.mapper;

import com.proveedores.dto.ObjetoCategoriaRequestDTO;
import com.proveedores.dto.ObjetoCategoriaResponseDTO;
import com.proveedores.entity.ObjetoCategoria;

public final class ObjetoCategoriaMapper {
    private ObjetoCategoriaMapper() {
    }

    public static ObjetoCategoria toEntity(ObjetoCategoriaRequestDTO dto) {
        ObjetoCategoria entity = new ObjetoCategoria();
        entity.setObjetoMuseo(MapperReferences.objetoMuseo(dto.objetoMuseoId()));
        entity.setCategoriaObjeto(MapperReferences.categoriaObjeto(dto.categoriaId()));
        entity.setObservaciones(dto.observaciones());
        return entity;
    }

    public static ObjetoCategoriaResponseDTO toResponse(ObjetoCategoria entity) {
        return new ObjetoCategoriaResponseDTO(entity.getId(), entity.getObjetoMuseo().getId(), entity.getObjetoMuseo().getDenominacionObjeto(), entity.getCategoriaObjeto().getId(), entity.getCategoriaObjeto().getNombre(), entity.getObservaciones());
    }
}
