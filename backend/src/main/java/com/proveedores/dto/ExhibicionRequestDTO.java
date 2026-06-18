package com.proveedores.dto;

import com.proveedores.entity.EstadoExhibicion;
import com.proveedores.entity.TipoExhibicion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;

public record ExhibicionRequestDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 160, message = "El nombre no puede superar 160 caracteres")
        String nombre,

        String descripcion,

        @NotNull(message = "El tipo de exhibicion es obligatorio")
        TipoExhibicion tipo,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        LocalDate fechaFin,

        @NotNull(message = "El estado es obligatorio")
        EstadoExhibicion estado,

        Set<Long> objetoIds
) {
    public ExhibicionRequestDTO(String nombre, String descripcion, TipoExhibicion tipo, LocalDate fechaInicio, LocalDate fechaFin, EstadoExhibicion estado) {
        this(nombre, descripcion, tipo, fechaInicio, fechaFin, estado, null);
    }
}
