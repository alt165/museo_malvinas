package com.proveedores.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ObjetoDigitalRequestDTO(
        @NotBlank(message = "El numero de inventario es obligatorio")
        @Size(max = 80, message = "El numero de inventario no puede superar 80 caracteres")
        String numeroInventario,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 160, message = "El nombre no puede superar 160 caracteres")
        String nombre,

        @Size(max = 100, message = "El tipo de objeto no puede superar 100 caracteres")
        String tipoObjeto,

        String descripcion,

        @Size(max = 80, message = "El formato digital no puede superar 80 caracteres")
        String formatoDigital,

        @Size(max = 120, message = "El identificador digital no puede superar 120 caracteres")
        String identificadorDigital,

        String metadatos
) {
}
