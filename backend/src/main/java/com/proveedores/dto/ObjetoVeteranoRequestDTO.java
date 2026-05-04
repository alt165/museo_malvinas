package com.proveedores.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ObjetoVeteranoRequestDTO(
        @NotNull(message = "El objeto de museo es obligatorio")
        Long objetoMuseoId,

        @NotNull(message = "El veterano es obligatorio")
        Long veteranoId,

        @NotBlank(message = "El tipo de relacion es obligatorio")
        @Size(max = 100, message = "El tipo de relacion no puede superar 100 caracteres")
        String tipoRelacion,

        String descripcion
) {
}
