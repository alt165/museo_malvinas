package com.proveedores.dto;

import com.proveedores.entity.Fuerza;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record VeteranoRequestDTO(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 100) String apellido,
        @NotNull Fuerza fuerza,
        @PastOrPresent LocalDate fechaNacimiento,
        @PastOrPresent LocalDate fechaFallecimiento,
        String historia
) {
}
