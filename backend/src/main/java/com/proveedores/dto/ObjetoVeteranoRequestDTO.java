package com.proveedores.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ObjetoVeteranoRequestDTO(
        @NotNull Long objetoMuseoId,
        @NotNull Long veteranoId,
        @NotBlank @Size(max = 100) String tipoRelacion,
        String descripcion
) {
}
