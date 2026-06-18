package com.proveedores.dto;

import java.util.List;

public record ObjetoGrafoResponseDTO(
        List<NodoGrafoObjetoDTO> nodes,
        List<AristaGrafoObjetoDTO> edges
) {
}
