package com.proveedores.dto;

public record RelacionObjetoPorObjetoResponseDTO(
        Long idRelacion,
        Long objetoOrigenId,
        String objetoOrigenNumeroInventario,
        String objetoOrigenNombre,
        Long objetoDestinoId,
        String objetoDestinoNumeroInventario,
        String objetoDestinoNombre,
        String tipoRelacion,
        String descripcion,
        String direccion
) {
}
