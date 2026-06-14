package com.proveedores.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ActuacionVeteranoRequestDTO(
        @NotNull(message = "El veterano es obligatorio")
        Long veteranoId,

        @Size(max = 80, message = "El rango no puede superar 80 caracteres")
        String rango,

        @Size(max = 120, message = "La unidad no puede superar 120 caracteres")
        String unidad,

        Long rangoId,

        Long unidadId,

        @Size(max = 120, message = "El rol no puede superar 120 caracteres")
        String rol,

        @PastOrPresent(message = "La fecha de inicio no puede ser futura")
        LocalDate fechaInicio,

        @PastOrPresent(message = "La fecha de fin no puede ser futura")
        LocalDate fechaFin,

        String descripcion
) {
}
