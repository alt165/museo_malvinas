package com.proveedores.dto;

import com.proveedores.entity.Fuerza;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record VeteranoRequestDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede superar 100 caracteres")
        String apellido,

        @NotNull(message = "La fuerza es obligatoria")
        Fuerza fuerza,

        @PastOrPresent(message = "La fecha de nacimiento no puede ser futura")
        LocalDate fechaNacimiento,

        @PastOrPresent(message = "La fecha de fallecimiento no puede ser futura")
        LocalDate fechaFallecimiento,

        String historia
) {
}
