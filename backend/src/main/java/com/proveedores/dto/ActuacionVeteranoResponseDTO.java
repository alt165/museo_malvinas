package com.proveedores.dto;

import java.time.LocalDate;

public record ActuacionVeteranoResponseDTO(
        Long id,
        Long veteranoId,
        String veteranoNombreCompleto,
        String rango,
        String unidad,
        String rol,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String descripcion
) {
}
