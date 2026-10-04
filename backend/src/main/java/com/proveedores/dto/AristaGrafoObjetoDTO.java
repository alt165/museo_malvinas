package com.proveedores.dto;

public record AristaGrafoObjetoDTO(
        String id,
        String source,
        String target,
        TipoVinculoRelacion tipoVinculo,
        String tipoRelacion,
        String descripcion
) {
}
