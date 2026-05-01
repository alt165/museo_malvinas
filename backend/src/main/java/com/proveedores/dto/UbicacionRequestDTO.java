package com.proveedores.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UbicacionRequestDTO(
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 80) String tipo,
        String descripcion
) {
}
