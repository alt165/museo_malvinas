package com.proveedores.dto;

public record ObjetoCategoriaResponseDTO(
        Long id,
        Long objetoMuseoId,
        String objetoNombre,
        Long categoriaId,
        String categoriaNombre,
        String observaciones
) {
}
