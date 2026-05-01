package com.proveedores.dto;

import com.proveedores.entity.Fuerza;
import java.time.LocalDate;

public record VeteranoResponseDTO(
        Long id,
        String nombre,
        String apellido,
        String nombreCompleto,
        Fuerza fuerza,
        LocalDate fechaNacimiento,
        LocalDate fechaFallecimiento,
        String historia
) {
}
