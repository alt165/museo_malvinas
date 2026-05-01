package com.proveedores.dto;

import com.proveedores.entity.TipoMovimientoInventario;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDateTime;

public record MovimientoInventarioRequestDTO(
        @NotNull Long objetoMuseoId,
        @NotNull TipoMovimientoInventario tipo,
        @NotNull @PastOrPresent LocalDateTime fecha,
        Long ubicacionOrigenId,
        Long ubicacionDestinoId,
        Long usuarioId,
        String observaciones
) {
}
