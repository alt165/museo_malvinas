package com.proveedores.dto;

import com.proveedores.entity.TipoMovimientoInventario;
import java.time.LocalDateTime;

public record MovimientoInventarioResponseDTO(
        Long id,
        Long objetoMuseoId,
        String objetoNombre,
        TipoMovimientoInventario tipo,
        LocalDateTime fecha,
        Long ubicacionOrigenId,
        String ubicacionOrigenNombre,
        Long ubicacionDestinoId,
        String ubicacionDestinoNombre,
        Long usuarioId,
        String usuarioNombre,
        String observaciones
) {
}
