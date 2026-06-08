package com.proveedores.dto;

import java.time.LocalDateTime;

public record HistorialObjetoResponseDTO(
        Long id,
        LocalDateTime fechaHora,
        String tipoOperacion,
        String accion,
        String descripcion,
        String usuario,
        String rol,
        String origen,
        String valoresAnteriores,
        String valoresNuevos
) {
}
