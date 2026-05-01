package com.proveedores.dto;

import com.proveedores.entity.EstadoConservacion;
import com.proveedores.entity.EstadoInventario;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InventarioResponseDTO(
        Long id,
        Long objetoMuseoId,
        String objetoNombre,
        Long ubicacionId,
        String ubicacionNombre,
        EstadoInventario estado,
        EstadoConservacion estadoConservacion,
        LocalDate fechaIngreso,
        LocalDate fechaSalida,
        LocalDateTime fechaUltimoMovimiento,
        String observaciones
) {
}
