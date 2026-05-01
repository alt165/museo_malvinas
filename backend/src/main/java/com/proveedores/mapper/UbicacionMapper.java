package com.proveedores.mapper;

import com.proveedores.dto.UbicacionRequestDTO;
import com.proveedores.dto.UbicacionResponseDTO;
import com.proveedores.entity.Ubicacion;

public final class UbicacionMapper {
    private UbicacionMapper() {
    }

    public static Ubicacion toEntity(UbicacionRequestDTO dto) {
        Ubicacion entity = new Ubicacion();
        entity.setNombre(dto.nombre());
        entity.setTipo(dto.tipo());
        entity.setDescripcion(dto.descripcion());
        return entity;
    }

    public static UbicacionResponseDTO toResponse(Ubicacion entity) {
        return new UbicacionResponseDTO(entity.getId(), entity.getNombre(), entity.getTipo(), entity.getDescripcion());
    }
}
