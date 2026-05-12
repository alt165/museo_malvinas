package com.proveedores.dto;

import com.proveedores.entity.EstadoConservacion;
import java.time.LocalDateTime;
import java.util.List;

public record ObjetoMuseoEliminadoResponseDTO(
        Long id,
        String numeroInventario,
        String denominacionObjeto,
        String descripcion,
        LocalDateTime fechaEliminacion,
        String eliminadoPor,
        EstadoConservacion estadoConservacion,
        List<CategoriaObjetoResponseDTO> categorias
) {
}
