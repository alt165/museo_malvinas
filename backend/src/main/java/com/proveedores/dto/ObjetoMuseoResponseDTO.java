package com.proveedores.dto;

import com.proveedores.entity.EstadoConservacion;
import com.proveedores.entity.OrigenCargaObjeto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ObjetoMuseoResponseDTO(
        Long id,
        String numeroInventario,
        String denominacionObjeto,
        String descripcion,
        String descripcionTecnica,
        String materiales,
        String dimensiones,
        EstadoConservacion estadoConservacion,
        LocalDate fechaIngreso,
        OrigenCargaObjeto origenCarga,
        Boolean datosCompletos,
        LocalDateTime fechaCargaRapida,
        String cargaRapidaPor,
        List<CategoriaObjetoResponseDTO> categorias,
        List<FotoObjetoMuseoResponseDTO> fotos,
        ReciboEscaneadoObjetoMuseoResponseDTO reciboEscaneado
) {
}
