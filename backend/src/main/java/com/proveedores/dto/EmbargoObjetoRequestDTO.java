package com.proveedores.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record EmbargoObjetoRequestDTO(
        @NotNull(message = "El objeto es obligatorio")
        Long objetoMuseoId,
        LocalDate fechaInicio,
        LocalDate fechaFinalizacion,
        String observaciones
) {
}
