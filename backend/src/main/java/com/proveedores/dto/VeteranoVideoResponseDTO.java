package com.proveedores.dto;

import java.time.LocalDate;

public record VeteranoVideoResponseDTO(
        Long id,
        Long veteranoId,
        String titulo,
        String urlYoutube,
        String videoId,
        String descripcion,
        LocalDate fechaEntrevista,
        Integer orden
) {
}
