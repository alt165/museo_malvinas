package com.proveedores.dto;

public record ColeccionObjetoResponseDTO(
        Long id,
        String nombre,
        String descripcion,
        Boolean activo,
        Long cantidadObjetos
) {
}
