package com.proveedores.dto;

import java.time.LocalDate;

public record ActuacionVeteranoResponseDTO(
        Long id,
        Long veteranoId,
        String veteranoNombreCompleto,
        String rango,
        String unidad,
        Long rangoId,
        String rangoNombre,
        Long unidadId,
        String unidadNombre,
        String unidadSigla,
        String rol,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String descripcion
) {
}
