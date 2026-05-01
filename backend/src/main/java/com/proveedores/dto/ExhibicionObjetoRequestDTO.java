package com.proveedores.dto;

import com.proveedores.entity.EstadoExhibicionObjeto;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExhibicionObjetoRequestDTO(
        @NotNull Long exhibicionId,
        @NotNull Long objetoMuseoId,
        @NotNull LocalDate fechaInclusion,
        LocalDate fechaRetiro,
        @NotNull EstadoExhibicionObjeto estado,
        Boolean devolucionVerificada,
        Long verificadoPorUsuarioId,
        LocalDateTime fechaVerificacion,
        String observacionesDevolucion
) {
}
