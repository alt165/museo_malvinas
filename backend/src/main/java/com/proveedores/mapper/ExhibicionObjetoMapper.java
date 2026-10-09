package com.proveedores.mapper;

import com.proveedores.dto.ExhibicionObjetoResponseDTO;
import com.proveedores.entity.ExhibicionObjeto;

public final class ExhibicionObjetoMapper {
    private ExhibicionObjetoMapper() {
    }

    public static ExhibicionObjetoResponseDTO toResponse(ExhibicionObjeto entity) {
        return new ExhibicionObjetoResponseDTO(entity.getId(), entity.getExhibicion().getId(), entity.getExhibicion().getNombre(), entity.getObjetoMuseo().getId(), entity.getObjetoMuseo().getNumeroInventario(), entity.getObjetoMuseo().getDenominacionObjeto(), entity.getFechaInclusion(), entity.getFechaRetiro(), entity.getEstado(), entity.getDevolucionVerificada(), userId(entity), userName(entity), entity.getFechaVerificacion(), entity.getObservacionesDevolucion());
    }

    private static Long userId(ExhibicionObjeto entity) { return entity.getVerificadoPor() == null ? null : entity.getVerificadoPor().getId(); }
    private static String userName(ExhibicionObjeto entity) { return entity.getVerificadoPor() == null ? null : entity.getVerificadoPor().getNombre(); }
}
