package com.proveedores.dto;

import java.time.LocalDate;

public record ObjetoDepositanteResponseDTO(
        Long id,
        Long objetoMuseoId,
        String objetoNombre,
        Long depositanteId,
        String depositanteNombre,
        LocalDate fechaDeposito,
        String tipoDeposito,
        String observaciones
) {
}
