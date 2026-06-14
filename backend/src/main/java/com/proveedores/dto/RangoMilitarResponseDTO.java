package com.proveedores.dto;

import com.proveedores.entity.Fuerza;

public record RangoMilitarResponseDTO(
        Long id,
        Fuerza fuerza,
        String nombre,
        Integer ordenJerarquico
) {
}
