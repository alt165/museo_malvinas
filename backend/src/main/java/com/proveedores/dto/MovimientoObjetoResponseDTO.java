package com.proveedores.dto;

import com.proveedores.entity.TipoMovimientoInventario;
import java.time.LocalDateTime;

public record MovimientoObjetoResponseDTO(
        Long id,
        LocalDateTime fechaMovimiento,
        Long ubicacionOrigenId,
        String ubicacionOrigen,
        Long ubicacionDestinoId,
        String ubicacionDestino,
        String descripcion,
        String usuarioMovimiento,
        TipoMovimientoInventario tipoMovimiento,
        String estadoAnterior,
        String estadoNuevo
) {
}
