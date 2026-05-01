package com.proveedores.dto;

import com.proveedores.entity.EstadoExhibicion;
import com.proveedores.entity.TipoExhibicion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ExhibicionRequestDTO(
        @NotBlank @Size(max = 160) String nombre,
        String descripcion,
        @NotNull TipoExhibicion tipo,
        @NotNull LocalDate fechaInicio,
        LocalDate fechaFin,
        @NotNull EstadoExhibicion estado
) {
}
