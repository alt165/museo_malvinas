package com.proveedores.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RelacionObjetoRequestDTO(
        @NotNull Long objetoOrigenId,
        @NotNull Long objetoDestinoId,
        @NotBlank @Size(max = 80) String tipoRelacion,
        String descripcion
) {
}
