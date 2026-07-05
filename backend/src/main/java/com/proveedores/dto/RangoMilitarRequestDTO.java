package com.proveedores.dto;

import com.proveedores.entity.Fuerza;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RangoMilitarRequestDTO(
        @NotNull(message = "La fuerza es obligatoria")
        Fuerza fuerza,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombre,

        @NotNull(message = "El orden jerarquico es obligatorio")
        @Min(value = 0, message = "El orden jerarquico no puede ser negativo")
        Integer ordenJerarquico
) {
}
