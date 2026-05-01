package com.proveedores.dto;

import com.proveedores.entity.EstadoExhibicionObjeto;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExhibicionObjetoResponseDTO(
        Long id,
        Long exhibicionId,
        String exhibicionNombre,
        Long objetoMuseoId,
        String objetoNombre,
        LocalDate fechaInclusion,
        LocalDate fechaRetiro,
        EstadoExhibicionObjeto estado,
        Boolean devolucionVerificada,
        Long verificadoPorUsuarioId,
        String verificadoPorNombre,
        LocalDateTime fechaVerificacion,
        String observacionesDevolucion
) {
}
