package com.proveedores.dto;

import com.proveedores.entity.CaracterRecepcionObjeto;
import java.time.LocalDate;

public record ComodatoPrestamoResponseDTO(
        Long id,
        String numeroInventario,
        String denominacionObjeto,
        Long depositanteId,
        String depositanteNombre,
        CaracterRecepcionObjeto caracterRecepcion,
        LocalDate fechaIngreso,
        LocalDate fechaVencimiento,
        Long diasRestantes,
        EstadoVencimientoComodatoPrestamo estadoVencimiento
) {
}
