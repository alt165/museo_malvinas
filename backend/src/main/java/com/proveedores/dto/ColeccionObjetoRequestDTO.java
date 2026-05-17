package com.proveedores.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ColeccionObjetoRequestDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 160, message = "El nombre no puede superar 160 caracteres")
        String nombre,

        String descripcion
) {
}
