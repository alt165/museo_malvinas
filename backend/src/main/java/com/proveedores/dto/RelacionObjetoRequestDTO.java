package com.proveedores.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RelacionObjetoRequestDTO(
        @NotNull(message = "El objeto origen es obligatorio")
        Long objetoOrigenId,

        @NotNull(message = "El objeto destino es obligatorio")
        Long objetoDestinoId,

        @NotBlank(message = "El tipo de relacion es obligatorio")
        @Size(max = 80, message = "El tipo de relacion no puede superar 80 caracteres")
        String tipoRelacion,

        String descripcion
) {
}
