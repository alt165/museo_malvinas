package com.proveedores.dto;

import java.time.LocalDateTime;

public record ReciboIngresoObjetoResponseDTO(
        Long id,
        String numeroRecibo,
        LocalDateTime fechaEmision,
        Long objetoMuseoId,
        Long depositanteId,
        String numeroInventario,
        String denominacionObjeto,
        String descripcionBreve,
        String depositanteNombre,
        String depositanteContacto,
        String operador,
        String textoConstancia,
        Boolean tieneCopiaFirmada,
        String copiaFirmadaNombreArchivo,
        LocalDateTime copiaFirmadaFechaCarga,
        String copiaFirmadaCargadoPor
) {
}
