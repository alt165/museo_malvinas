package com.proveedores.dto;

import com.proveedores.entity.EstadoExhibicionObjeto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExhibicionObjetoRequestDTO(
        @NotNull(message = "La exhibicion es obligatoria")
        Long exhibicionId,

        @NotNull(message = "El objeto de museo es obligatorio")
        Long objetoMuseoId,

        @NotNull(message = "La fecha de inclusion es obligatoria")
        @PastOrPresent(message = "La fecha de inclusion no puede ser futura")
        LocalDate fechaInclusion,

        LocalDate fechaRetiro,

        @NotNull(message = "El estado es obligatorio")
        EstadoExhibicionObjeto estado,

        Boolean devolucionVerificada,
        Long verificadoPorUsuarioId,

        @PastOrPresent(message = "La fecha de verificacion no puede ser futura")
        LocalDateTime fechaVerificacion,

        String observacionesDevolucion
) {
}
