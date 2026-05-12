package com.proveedores.dto;

import com.proveedores.entity.EstadoConservacion;
import java.time.LocalDate;
import java.util.List;

public record ObjetoMuseoResponseDTO(
        Long id,
        String numeroInventario,
        String denominacionObjeto,
        String descripcion,
        String descripcionTecnica,
        String materiales,
        String dimensiones,
        EstadoConservacion estadoConservacion,
        LocalDate fechaIngreso,
        List<CategoriaObjetoResponseDTO> categorias
) {
}
