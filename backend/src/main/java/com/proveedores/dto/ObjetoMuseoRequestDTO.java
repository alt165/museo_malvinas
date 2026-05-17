package com.proveedores.dto;

import com.proveedores.entity.EstadoConservacion;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record ObjetoMuseoRequestDTO(
        @NotBlank(message = "El numero de inventario es obligatorio")
        @Size(max = 80, message = "El numero de inventario no puede superar 80 caracteres")
        String numeroInventario,

        @Size(max = 160, message = "La denominacion no puede superar 160 caracteres")
        String denominacionObjeto,

        String descripcion,

        String descripcionTecnica,

        String materiales,

        String dimensiones,

        EstadoConservacion estadoConservacion,

        Set<Long> categoriaIds,

        Long ubicacionId
) {
    public ObjetoMuseoRequestDTO(
            String numeroInventario,
            String denominacionObjeto,
            String descripcion,
            String descripcionTecnica,
            String materiales,
            String dimensiones,
            EstadoConservacion estadoConservacion,
            Set<Long> categoriaIds
    ) {
        this(numeroInventario, denominacionObjeto, descripcion, descripcionTecnica, materiales, dimensiones, estadoConservacion, categoriaIds, null);
    }

    @AssertTrue(message = "La denominacion o nombre es obligatorio")
    public boolean isDenominacionONombreValida() {
        return denominacionObjeto != null && !denominacionObjeto.isBlank();
    }
}
