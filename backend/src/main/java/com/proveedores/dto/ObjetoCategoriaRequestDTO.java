package com.proveedores.dto;

import jakarta.validation.constraints.NotNull;

public record ObjetoCategoriaRequestDTO(@NotNull Long objetoMuseoId, @NotNull Long categoriaId, String observaciones) {
}
