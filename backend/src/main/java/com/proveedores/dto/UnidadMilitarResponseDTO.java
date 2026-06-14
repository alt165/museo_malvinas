package com.proveedores.dto;

import com.proveedores.entity.Fuerza;

public record UnidadMilitarResponseDTO(
        Long id,
        Fuerza fuerza,
        String nombre,
        String sigla,
        String tipoUnidad,
        String descripcion
) {
}
