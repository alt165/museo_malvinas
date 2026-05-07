package com.proveedores.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDTO(
        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, max = 120, message = "La contrasena debe tener entre 8 y 120 caracteres")
        String contrasena
) {
}
