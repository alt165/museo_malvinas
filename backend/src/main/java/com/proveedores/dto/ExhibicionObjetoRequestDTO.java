package com.proveedores.dto;

import jakarta.validation.constraints.NotNull;

public record ExhibicionObjetoRequestDTO(
        @NotNull(message = "La exhibicion es obligatoria")
        Long exhibicionId,

        @NotNull(message = "El objeto de museo es obligatorio")
        Long objetoMuseoId
) {
}
