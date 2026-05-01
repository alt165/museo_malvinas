package com.proveedores.dto;

import com.proveedores.entity.EstadoExhibicion;
import com.proveedores.entity.TipoExhibicion;
import java.time.LocalDate;

public record ExhibicionResponseDTO(
        Long id,
        String nombre,
        String descripcion,
        TipoExhibicion tipo,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        EstadoExhibicion estado
) {
}
