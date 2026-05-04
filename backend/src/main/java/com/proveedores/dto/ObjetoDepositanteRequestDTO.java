package com.proveedores.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ObjetoDepositanteRequestDTO(
        @NotNull(message = "El objeto de museo es obligatorio")
        Long objetoMuseoId,

        @NotNull(message = "El depositante es obligatorio")
        Long depositanteId,

        @PastOrPresent(message = "La fecha de deposito no puede ser futura")
        LocalDate fechaDeposito,

        @Size(max = 80, message = "El tipo de deposito no puede superar 80 caracteres")
        String tipoDeposito,

        String observaciones
) {
}
