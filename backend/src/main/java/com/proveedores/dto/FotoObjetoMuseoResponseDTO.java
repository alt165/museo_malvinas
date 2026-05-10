package com.proveedores.dto;

import java.time.LocalDateTime;

public record FotoObjetoMuseoResponseDTO(
        Long id,
        Long objetoMuseoId,
        String nombreArchivo,
        String contentType,
        Long tamanioBytes,
        String descripcion,
        LocalDateTime fechaCarga,
        String cargadoPor
) {
}
