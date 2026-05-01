package com.proveedores.dto;

public record ObjetoDigitalResponseDTO(
        Long id,
        String numeroInventario,
        String nombre,
        String tipoObjeto,
        String descripcion,
        String formatoDigital,
        String identificadorDigital,
        String metadatos
) {
}
