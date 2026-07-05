package com.proveedores.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DetalleConservacionRequestDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 160, message = "El nombre no puede superar 160 caracteres")
        String nombre,

        @Size(max = 80, message = "El codigo no puede superar 80 caracteres")
        @Pattern(regexp = "^[A-Z0-9_]*$", message = "El codigo solo puede contener mayusculas, numeros y guiones bajos")
        String codigo,

        String descripcion
) {
}
