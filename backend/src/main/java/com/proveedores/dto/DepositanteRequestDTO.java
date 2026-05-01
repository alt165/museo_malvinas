package com.proveedores.dto;

import com.proveedores.entity.TipoDepositante;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DepositanteRequestDTO(
        @NotBlank @Size(max = 160) String nombre,
        @NotNull TipoDepositante tipo,
        @Size(max = 160) String contacto,
        String observaciones
) {
}
