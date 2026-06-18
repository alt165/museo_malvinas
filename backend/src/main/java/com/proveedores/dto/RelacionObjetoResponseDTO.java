package com.proveedores.dto;

public record RelacionObjetoResponseDTO(
        Long id,
        Long objetoOrigenId,
        String objetoOrigenNumeroInventario,
        String objetoOrigenNombre,
        Long objetoDestinoId,
        String objetoDestinoNumeroInventario,
        String objetoDestinoNombre,
        String tipoRelacion,
        String descripcion,
        java.time.LocalDateTime fechaCreacion,
        String creadoPor
) {
}
