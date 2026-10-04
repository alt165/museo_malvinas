package com.proveedores.dto;

public record RelacionElementoResponseDTO(
        String idRelacion,
        TipoVinculoRelacion tipoVinculo,
        TipoNodoRelacion tipoElemento,
        Long elementoId,
        String numeroInventario,
        String denominacion,
        String tipoRelacion,
        String descripcion,
        String direccion
) {
}
