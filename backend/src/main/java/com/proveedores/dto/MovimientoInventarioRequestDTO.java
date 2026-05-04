package com.proveedores.dto;

import com.proveedores.entity.TipoMovimientoInventario;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDateTime;

public record MovimientoInventarioRequestDTO(
        @NotNull(message = "El objeto de museo es obligatorio")
        Long objetoMuseoId,

        @NotNull(message = "El tipo de movimiento es obligatorio")
        TipoMovimientoInventario tipo,

        @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura")
        LocalDateTime fecha,

        Long ubicacionOrigenId,
        Long ubicacionDestinoId,
        Long usuarioId,
        String observaciones
) {
}
