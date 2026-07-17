package com.proveedores.dto;

import com.proveedores.entity.VisibilidadCampo;
import java.time.LocalDateTime;

public record FotoObjetoMuseoResponseDTO(
        Long id,
        Long objetoMuseoId,
        String nombreArchivo,
        String nombreArchivoAlmacenado,
        String contentType,
        Long tamanioBytes,
        String descripcion,
        VisibilidadCampo visibilidad,
        LocalDateTime fechaCarga,
        String cargadoPor
) {
}
