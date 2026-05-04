package com.proveedores.dto;

import com.proveedores.entity.EstadoConservacion;
import com.proveedores.entity.EstadoInventario;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

public record InventarioRequestDTO(
        @NotNull(message = "El objeto de museo es obligatorio")
        Long objetoMuseoId,

        @NotNull(message = "La ubicacion es obligatoria")
        Long ubicacionId,

        @NotNull(message = "El estado de inventario es obligatorio")
        EstadoInventario estado,

        @NotNull(message = "El estado de conservacion es obligatorio")
        EstadoConservacion estadoConservacion,

        @NotNull(message = "La fecha de ingreso es obligatoria")
        @PastOrPresent(message = "La fecha de ingreso no puede ser futura")
        LocalDate fechaIngreso,

        @PastOrPresent(message = "La fecha de salida no puede ser futura")
        LocalDate fechaSalida,

        String observaciones
) {
}
