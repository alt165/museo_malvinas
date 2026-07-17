package com.proveedores.dto;

public record DetalleConservacionResponseDTO(
        Long id,
        String codigo,
        String nombre,
        String descripcion
) {
}
