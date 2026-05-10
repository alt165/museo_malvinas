package com.proveedores.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CargaRapidaObjetoRequestDTO(
        @NotNull(message = "El depositante es obligatorio")
        Long depositanteId,

        @NotBlank(message = "La denominacion es obligatoria")
        @Size(max = 160, message = "La denominacion no puede superar 160 caracteres")
        String denominacionObjeto,

        @NotBlank(message = "El numero de inventario es obligatorio")
        @Size(max = 80, message = "El numero de inventario no puede superar 80 caracteres")
        String numeroInventario,

        @NotBlank(message = "La descripcion breve es obligatoria")
        @Size(min = 5, message = "La descripcion breve debe tener al menos 5 caracteres")
        String descripcionBreve
) {
}
