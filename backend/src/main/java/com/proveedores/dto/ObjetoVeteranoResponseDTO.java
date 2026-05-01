package com.proveedores.dto;

public record ObjetoVeteranoResponseDTO(
        Long id,
        Long objetoMuseoId,
        String objetoNombre,
        Long veteranoId,
        String veteranoNombreCompleto,
        String tipoRelacion,
        String descripcion
) {
}
