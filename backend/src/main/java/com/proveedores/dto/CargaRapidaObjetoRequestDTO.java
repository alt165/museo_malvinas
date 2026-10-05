package com.proveedores.dto;

import com.proveedores.entity.CaracterRecepcionObjeto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CargaRapidaObjetoRequestDTO(
        @NotNull(message = "El depositante es obligatorio")
        Long depositanteId,

        @NotBlank(message = "La denominacion es obligatoria")
        @Size(max = 160, message = "La denominacion no puede superar 160 caracteres")
        String denominacionObjeto,


        @NotBlank(message = "La descripcion breve es obligatoria")
        @Size(min = 5, message = "La descripcion breve debe tener al menos 5 caracteres")
        String descripcionBreve,

        @NotNull(message = "El caracter de recepcion es obligatorio")
        CaracterRecepcionObjeto caracterRecepcion,

        LocalDate fechaVencimiento
) {
}
