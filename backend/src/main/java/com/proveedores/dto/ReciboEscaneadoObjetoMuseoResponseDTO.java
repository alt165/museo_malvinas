package com.proveedores.dto;

import java.time.LocalDateTime;

public record ReciboEscaneadoObjetoMuseoResponseDTO(
        Long id,
        Long objetoMuseoId,
        String nombreArchivoOriginal,
        String contentType,
        Long tamanioBytes,
        LocalDateTime fechaCarga,
        String cargadoPor
) {
}
