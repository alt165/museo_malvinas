package com.proveedores.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record VeteranoVideoRequestDTO(
        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 180, message = "El titulo no puede superar 180 caracteres")
        String titulo,

        @NotBlank(message = "La URL de YouTube es obligatoria")
        @Size(max = 500, message = "La URL de YouTube no puede superar 500 caracteres")
        String urlYoutube,

        String descripcion,

        @PastOrPresent(message = "La fecha de entrevista no puede ser futura")
        LocalDate fechaEntrevista,

        Integer orden
) {
}
