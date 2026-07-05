package com.proveedores.dto;

import com.proveedores.entity.Fuerza;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UnidadMilitarRequestDTO(
        @NotNull(message = "La fuerza es obligatoria")
        Fuerza fuerza,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 180, message = "El nombre no puede superar 180 caracteres")
        String nombre,

        @Size(max = 40, message = "La sigla no puede superar 40 caracteres")
        String sigla,

        @Size(max = 80, message = "El tipo de unidad no puede superar 80 caracteres")
        String tipoUnidad,

        String descripcion
) {
}
