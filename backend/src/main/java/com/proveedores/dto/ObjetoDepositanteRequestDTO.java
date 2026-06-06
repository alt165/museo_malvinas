package com.proveedores.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

public record ObjetoDepositanteRequestDTO(
        @NotNull(message = "El objeto de museo es obligatorio")
        Long objetoMuseoId,

        @NotNull(message = "El depositante es obligatorio")
        Long depositanteId,

        @PastOrPresent(message = "La fecha de deposito no puede ser futura")
        LocalDate fechaDeposito,

        com.proveedores.entity.CaracterRecepcionObjeto tipoDeposito,

        LocalDate fechaVencimiento,

        String observaciones
) {
}
