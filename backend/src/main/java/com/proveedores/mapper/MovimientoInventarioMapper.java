package com.proveedores.mapper;

import com.proveedores.dto.MovimientoInventarioResponseDTO;
import com.proveedores.entity.MovimientoInventario;

public final class MovimientoInventarioMapper {
    private MovimientoInventarioMapper() {
    }

    public static MovimientoInventarioResponseDTO toResponse(MovimientoInventario entity) {
        return new MovimientoInventarioResponseDTO(entity.getId(), entity.getObjetoMuseo().getId(), entity.getObjetoMuseo().getDenominacionObjeto(), entity.getTipo(), entity.getFecha(), id(entity.getUbicacionOrigen()), nombre(entity.getUbicacionOrigen()), id(entity.getUbicacionDestino()), nombre(entity.getUbicacionDestino()), userId(entity), userName(entity), entity.getObservaciones());
    }

    private static Long id(com.proveedores.entity.Ubicacion ubicacion) { return ubicacion == null ? null : ubicacion.getId(); }
    private static String nombre(com.proveedores.entity.Ubicacion ubicacion) { return ubicacion == null ? null : ubicacion.getNombre(); }
    private static Long userId(MovimientoInventario entity) { return entity.getUsuario() == null ? null : entity.getUsuario().getId(); }
    private static String userName(MovimientoInventario entity) { return entity.getUsuario() == null ? null : entity.getUsuario().getNombre(); }
}
