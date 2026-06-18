package com.proveedores.dto;

import com.proveedores.entity.CaracterRecepcionObjeto;
import java.time.LocalDate;

public record ObjetoVencimientoProximoResponseDTO(
        Long id,
        String numeroInventario,
        String denominacionObjeto,
        Long depositanteId,
        String depositanteNombre,
        CaracterRecepcionObjeto caracterRecepcion,
        LocalDate fechaVencimiento,
        long diasRestantes
) {
}
