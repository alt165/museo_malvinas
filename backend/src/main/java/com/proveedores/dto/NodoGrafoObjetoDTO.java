package com.proveedores.dto;

public record NodoGrafoObjetoDTO(
        String id,
        Long entidadId,
        TipoNodoRelacion tipo,
        String label,
        String numeroInventario
) {
}
