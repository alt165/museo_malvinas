package com.proveedores.dto;

import jakarta.validation.constraints.NotNull;

public record AgregarCategoriaObjetoRequestDTO(
        @NotNull(message = "La categoria es obligatoria")
        Long categoriaId,

        String observaciones
) {
}
