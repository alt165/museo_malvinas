package com.proveedores.dto;

import java.time.LocalDate;

public record ObjetoDisponibilidadExhibicionResponseDTO(
        Long objetoId,
        String numeroInventario,
        String denominacion,
        boolean disponible,
        String motivoNoDisponible,
        Long exhibicionConflictoId,
        String exhibicionConflictoNombre,
        LocalDate exhibicionConflictoFechaInicio,
        LocalDate exhibicionConflictoFechaFin,
        boolean exhibicionConflictoPermanente
) {
}
