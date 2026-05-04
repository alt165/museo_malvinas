package com.proveedores.dto;

import jakarta.validation.constraints.NotNull;

public record ObjetoCategoriaRequestDTO(
        @NotNull(message = "El objeto de museo es obligatorio")
        Long objetoMuseoId,

        @NotNull(message = "La categoria es obligatoria")
        Long categoriaId,

        String observaciones
) {
}
