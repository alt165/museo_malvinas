package com.proveedores.mapper;

import com.proveedores.dto.DepositanteRequestDTO;
import com.proveedores.dto.DepositanteResponseDTO;
import com.proveedores.entity.Depositante;

public final class DepositanteMapper {
    private DepositanteMapper() {
    }

    public static Depositante toEntity(DepositanteRequestDTO dto) {
        Depositante entity = new Depositante();
        entity.setNombre(dto.nombre());
        entity.setTipo(dto.tipo());
        entity.setContacto(dto.contacto());
        entity.setObservaciones(dto.observaciones());
        return entity;
    }

    public static DepositanteResponseDTO toResponse(Depositante entity) {
        return new DepositanteResponseDTO(entity.getId(), entity.getNombre(), entity.getTipo(), entity.getContacto(), entity.getObservaciones());
    }
}
