package com.proveedores.dto;

import java.time.LocalDate;

public record EmbargoObjetoResponseDTO(
        Long id,
        Long objetoMuseoId,
        String numeroInventario,
        String denominacionObjeto,
        LocalDate fechaInicio,
        LocalDate fechaFinalizacion,
        String estado,
        String observaciones
) {
}
