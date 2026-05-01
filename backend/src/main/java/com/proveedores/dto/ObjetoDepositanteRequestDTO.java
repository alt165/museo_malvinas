package com.proveedores.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ObjetoDepositanteRequestDTO(
        @NotNull Long objetoMuseoId,
        @NotNull Long depositanteId,
        @PastOrPresent LocalDate fechaDeposito,
        @Size(max = 80) String tipoDeposito,
        String observaciones
) {
}
