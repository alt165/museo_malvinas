package com.proveedores.dto;

public record ObjetoMuseoResponseDTO(
        Long id,
        String numeroInventario,
        String nombre,
        String tipoObjeto,
        String descripcion
) {
}
