package com.proveedores.dto;

import jakarta.validation.constraints.NotNull;

public record MoverObjetoRequestDTO(
        @NotNull(message = "La ubicacion destino es obligatoria")
        Long ubicacionDestinoId,

        String descripcion
) {
}
