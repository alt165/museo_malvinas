package com.proveedores.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ActualizarFechaVencimientoRequestDTO(
        @NotNull(message = "La fecha de vencimiento es obligatoria")
        LocalDate fechaVencimiento
) {
}
