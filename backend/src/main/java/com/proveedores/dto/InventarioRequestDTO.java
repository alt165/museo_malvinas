package com.proveedores.dto;

import com.proveedores.entity.EstadoConservacion;
import com.proveedores.entity.EstadoInventario;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

public record InventarioRequestDTO(
        @NotNull Long objetoMuseoId,
        @NotNull Long ubicacionId,
        @NotNull EstadoInventario estado,
        @NotNull EstadoConservacion estadoConservacion,
        @NotNull @PastOrPresent LocalDate fechaIngreso,
        LocalDate fechaSalida,
        String observaciones
) {
}
