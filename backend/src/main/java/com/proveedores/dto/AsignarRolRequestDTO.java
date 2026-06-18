package com.proveedores.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record AsignarRolRequestDTO(
        @NotEmpty(message = "Debe indicar al menos un rol")
        Set<String> roles,

        Boolean confirmarQuitarAdminPropio
) {
}
