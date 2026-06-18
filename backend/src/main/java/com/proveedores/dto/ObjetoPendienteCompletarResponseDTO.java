package com.proveedores.dto;

import com.proveedores.entity.CaracterRecepcionObjeto;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ObjetoPendienteCompletarResponseDTO(
        Long id,
        String numeroInventario,
        String denominacionObjeto,
        String descripcion,
        Long depositanteId,
        String depositanteNombre,
        CaracterRecepcionObjeto caracterRecepcion,
        LocalDate fechaVencimiento,
        LocalDateTime fechaCargaRapida,
        String cargaRapidaPor,
        Long reciboId,
        String reciboPdfUrl
) {
}
