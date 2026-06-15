package com.proveedores.mapper;

import com.proveedores.dto.ExhibicionRequestDTO;
import com.proveedores.dto.ExhibicionResponseDTO;
import com.proveedores.entity.Exhibicion;

public final class ExhibicionMapper {
    private ExhibicionMapper() {
    }

    public static Exhibicion toEntity(ExhibicionRequestDTO dto) {
        Exhibicion entity = new Exhibicion();
        entity.setNombre(dto.nombre());
        entity.setDescripcion(dto.descripcion());
        entity.setTipo(dto.tipo());
        entity.setFechaInicio(dto.fechaInicio());
        entity.setFechaFin(dto.fechaFin());
        entity.setEstado(dto.estado());
        return entity;
    }

    public static ExhibicionResponseDTO toResponse(Exhibicion entity) {
        return new ExhibicionResponseDTO(entity.getId(), entity.getNombre(), entity.getDescripcion(), entity.getTipo(), entity.getFechaInicio(), entity.getFechaFin(), entity.getEstado(), entity.getFechaFin() == null, java.util.List.of());
    }

    public static ExhibicionResponseDTO toResponse(Exhibicion entity, java.util.List<com.proveedores.dto.ExhibicionObjetoResponseDTO> objetos) {
        return new ExhibicionResponseDTO(entity.getId(), entity.getNombre(), entity.getDescripcion(), entity.getTipo(), entity.getFechaInicio(), entity.getFechaFin(), entity.getEstado(), entity.getFechaFin() == null, objetos);
    }
}
