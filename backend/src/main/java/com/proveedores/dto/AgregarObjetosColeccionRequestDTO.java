package com.proveedores.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AgregarObjetosColeccionRequestDTO(
        @NotEmpty(message = "Debe seleccionar al menos un objeto")
        List<Long> objetoIds
) {
}
