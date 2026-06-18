package com.proveedores.mapper;

import com.proveedores.dto.RelacionObjetoRequestDTO;
import com.proveedores.dto.RelacionObjetoResponseDTO;
import com.proveedores.entity.RelacionObjeto;

public final class RelacionObjetoMapper {
    private RelacionObjetoMapper() {
    }

    public static RelacionObjeto toEntity(RelacionObjetoRequestDTO dto) {
        RelacionObjeto entity = new RelacionObjeto();
        entity.setObjetoOrigen(MapperReferences.objetoMuseo(dto.objetoOrigenId()));
        entity.setObjetoDestino(MapperReferences.objetoMuseo(dto.objetoDestinoId()));
        entity.setTipoRelacion(dto.tipoRelacion());
        entity.setDescripcion(dto.descripcion());
        return entity;
    }

    public static RelacionObjetoResponseDTO toResponse(RelacionObjeto entity) {
        return new RelacionObjetoResponseDTO(
                entity.getId(),
                entity.getObjetoOrigen().getId(),
                entity.getObjetoOrigen().getNumeroInventario(),
                entity.getObjetoOrigen().getDenominacionObjeto(),
                entity.getObjetoDestino().getId(),
                entity.getObjetoDestino().getNumeroInventario(),
                entity.getObjetoDestino().getDenominacionObjeto(),
                entity.getTipoRelacion(),
                entity.getDescripcion(),
                entity.getFechaCreacion(),
                entity.getCreadoPor()
        );
    }
}
