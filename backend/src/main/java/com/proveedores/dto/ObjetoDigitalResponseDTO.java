package com.proveedores.dto;

public record ObjetoDigitalResponseDTO(
        Long id,
        String numeroInventario,
        String denominacionObjeto,
        String descripcion,
        String formatoDigital,
        String identificadorDigital,
        String metadatos
) {
}
