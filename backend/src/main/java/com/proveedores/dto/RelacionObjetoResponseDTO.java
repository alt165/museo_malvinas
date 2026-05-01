package com.proveedores.dto;

public record RelacionObjetoResponseDTO(
        Long id,
        Long objetoOrigenId,
        String objetoOrigenNombre,
        Long objetoDestinoId,
        String objetoDestinoNombre,
        String tipoRelacion,
        String descripcion
) {
}
