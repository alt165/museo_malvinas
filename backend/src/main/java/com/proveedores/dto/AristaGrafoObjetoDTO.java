package com.proveedores.dto;

public record AristaGrafoObjetoDTO(
        Long id,
        Long source,
        Long target,
        String tipoRelacion,
        String descripcion
) {
}
