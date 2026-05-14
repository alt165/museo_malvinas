package com.proveedores.dto;

import java.time.LocalDateTime;

public record ObjetoPendienteCompletarResponseDTO(
        Long id,
        String numeroInventario,
        String denominacionObjeto,
        String descripcion,
        Long depositanteId,
        String depositanteNombre,
        LocalDateTime fechaCargaRapida,
        String cargaRapidaPor,
        Long reciboId,
        String reciboPdfUrl
) {
}
