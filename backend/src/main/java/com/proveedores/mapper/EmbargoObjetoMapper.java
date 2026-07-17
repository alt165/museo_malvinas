package com.proveedores.mapper;

import com.proveedores.dto.EmbargoObjetoResponseDTO;
import com.proveedores.entity.EmbargoObjeto;

public final class EmbargoObjetoMapper {
    private EmbargoObjetoMapper() {
    }

    public static EmbargoObjetoResponseDTO toResponse(EmbargoObjeto entity) {
        return new EmbargoObjetoResponseDTO(
                entity.getId(),
                entity.getObjetoMuseo().getId(),
                entity.getObjetoMuseo().getNumeroInventario(),
                entity.getObjetoMuseo().getDenominacionObjeto(),
                entity.getFechaInicio(),
                entity.getFechaFinalizacion(),
                entity.getFechaFinalizacion() == null ? "VIGENTE" : "LEVANTADO",
                entity.getObservaciones()
        );
    }
}
