package com.proveedores.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ObjetoDigitalRequestDTO(
        @NotBlank @Size(max = 80) String numeroInventario,
        @NotBlank @Size(max = 160) String nombre,
        @Size(max = 100) String tipoObjeto,
        String descripcion,
        @Size(max = 80) String formatoDigital,
        @Size(max = 120) String identificadorDigital,
        String metadatos
) {
}
