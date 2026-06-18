package com.proveedores.dto;

import com.proveedores.entity.TipoExhibicion;
import java.time.LocalDate;

public record ExhibicionProximaInicioResponseDTO(
        Long id,
        String nombre,
        LocalDate fechaInicio,
        long diasRestantes,
        TipoExhibicion tipo,
        boolean permanente
) {
}
