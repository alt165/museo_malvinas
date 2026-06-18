package com.proveedores.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ConfigAlertasVencimientoDTO(
        @Min(value = 1, message = "Los dias de anticipacion deben ser al menos 1")
        @Max(value = 365, message = "Los dias de anticipacion no pueden superar 365")
        int diasAnticipacion
) {
}
