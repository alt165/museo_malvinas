package com.proveedores.dto;

import java.time.LocalDateTime;

public record VeteranoImagenResponseDTO(
        Long id,
        Long veteranoId,
        String nombreArchivo,
        String nombreArchivoAlmacenado,
        String tipoContenido,
        Long tamanioBytes,
        String descripcion,
        Integer orden,
        LocalDateTime fechaCarga,
        String cargadoPor
) {
}
