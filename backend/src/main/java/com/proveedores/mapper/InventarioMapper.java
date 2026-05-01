package com.proveedores.mapper;

import com.proveedores.dto.InventarioRequestDTO;
import com.proveedores.dto.InventarioResponseDTO;
import com.proveedores.entity.Inventario;
import java.time.LocalDateTime;

public final class InventarioMapper {
    private InventarioMapper() {
    }

    public static Inventario toEntity(InventarioRequestDTO dto) {
        Inventario entity = new Inventario();
        entity.setObjetoMuseo(MapperReferences.objetoMuseo(dto.objetoMuseoId()));
        entity.setUbicacion(MapperReferences.ubicacion(dto.ubicacionId()));
        entity.setEstado(dto.estado());
        entity.setEstadoConservacion(dto.estadoConservacion());
        entity.setFechaIngreso(dto.fechaIngreso());
        entity.setFechaSalida(dto.fechaSalida());
        entity.setFechaUltimoMovimiento(LocalDateTime.now());
        entity.setObservaciones(dto.observaciones());
        return entity;
    }

    public static InventarioResponseDTO toResponse(Inventario entity) {
        return new InventarioResponseDTO(entity.getId(), entity.getObjetoMuseo().getId(), entity.getObjetoMuseo().getNombre(), entity.getUbicacion().getId(), entity.getUbicacion().getNombre(), entity.getEstado(), entity.getEstadoConservacion(), entity.getFechaIngreso(), entity.getFechaSalida(), entity.getFechaUltimoMovimiento(), entity.getObservaciones());
    }
}
