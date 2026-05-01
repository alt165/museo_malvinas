package com.proveedores.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ActuacionVeteranoRequestDTO(
        @NotNull Long veteranoId,
        @Size(max = 80) String rango,
        @Size(max = 120) String unidad,
        @Size(max = 120) String rol,
        @PastOrPresent LocalDate fechaInicio,
        @PastOrPresent LocalDate fechaFin,
        String descripcion
) {
}
