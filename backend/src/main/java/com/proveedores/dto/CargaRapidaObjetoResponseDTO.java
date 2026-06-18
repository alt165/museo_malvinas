package com.proveedores.dto;

public record CargaRapidaObjetoResponseDTO(
        ObjetoMuseoResponseDTO objeto,
        ReciboIngresoObjetoResponseDTO recibo,
        String reciboPdfUrl
) {
}
