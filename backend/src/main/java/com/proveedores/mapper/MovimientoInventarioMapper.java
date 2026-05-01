package com.proveedores.mapper;

import com.proveedores.dto.MovimientoInventarioRequestDTO;
import com.proveedores.dto.MovimientoInventarioResponseDTO;
import com.proveedores.entity.MovimientoInventario;

public final class MovimientoInventarioMapper {
    private MovimientoInventarioMapper() {
    }

    public static MovimientoInventario toEntity(MovimientoInventarioRequestDTO dto) {
        MovimientoInventario entity = new MovimientoInventario();
        entity.setObjetoMuseo(MapperReferences.objetoMuseo(dto.objetoMuseoId()));
        entity.setTipo(dto.tipo());
        entity.setFecha(dto.fecha());
        entity.setUbicacionOrigen(MapperReferences.ubicacion(dto.ubicacionOrigenId()));
        entity.setUbicacionDestino(MapperReferences.ubicacion(dto.ubicacionDestinoId()));
        entity.setUsuario(MapperReferences.usuario(dto.usuarioId()));
        entity.setObservaciones(dto.observaciones());
        return entity;
    }

    public static MovimientoInventarioResponseDTO toResponse(MovimientoInventario entity) {
        return new MovimientoInventarioResponseDTO(entity.getId(), entity.getObjetoMuseo().getId(), entity.getObjetoMuseo().getNombre(), entity.getTipo(), entity.getFecha(), id(entity.getUbicacionOrigen()), nombre(entity.getUbicacionOrigen()), id(entity.getUbicacionDestino()), nombre(entity.getUbicacionDestino()), userId(entity), userName(entity), entity.getObservaciones());
    }

    private static Long id(com.proveedores.entity.Ubicacion ubicacion) { return ubicacion == null ? null : ubicacion.getId(); }
    private static String nombre(com.proveedores.entity.Ubicacion ubicacion) { return ubicacion == null ? null : ubicacion.getNombre(); }
    private static Long userId(MovimientoInventario entity) { return entity.getUsuario() == null ? null : entity.getUsuario().getId(); }
    private static String userName(MovimientoInventario entity) { return entity.getUsuario() == null ? null : entity.getUsuario().getNombre(); }
}
