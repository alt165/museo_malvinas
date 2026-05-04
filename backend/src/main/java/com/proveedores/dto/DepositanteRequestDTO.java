package com.proveedores.dto;

import com.proveedores.entity.TipoDepositante;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DepositanteRequestDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 160, message = "El nombre no puede superar 160 caracteres")
        String nombre,

        @NotNull(message = "El tipo de depositante es obligatorio")
        TipoDepositante tipo,

        @Email(message = "El contacto debe tener formato de email")
        @Size(max = 160, message = "El contacto no puede superar 160 caracteres")
        String contacto,

        String observaciones
) {
}
