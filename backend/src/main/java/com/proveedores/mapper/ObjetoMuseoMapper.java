package com.proveedores.mapper;

import com.proveedores.dto.CategoriaObjetoResponseDTO;
import com.proveedores.dto.FotoObjetoMuseoResponseDTO;
import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.dto.ReciboEscaneadoObjetoMuseoResponseDTO;
import com.proveedores.entity.ObjetoMuseo;
import java.time.LocalDate;
import java.util.List;

public final class ObjetoMuseoMapper {
    private ObjetoMuseoMapper() {
    }

    public static ObjetoMuseo toEntity(ObjetoMuseoRequestDTO dto) {
        ObjetoMuseo entity = new ObjetoMuseo();
        updateEntity(entity, dto);
        return entity;
    }

    public static ObjetoMuseoResponseDTO toResponse(ObjetoMuseo entity) {
        return toResponse(entity, List.of());
    }

    public static ObjetoMuseoResponseDTO toResponse(ObjetoMuseo entity, List<CategoriaObjetoResponseDTO> categorias) {
        return toResponse(entity, null, categorias, List.of(), null);
    }

    public static ObjetoMuseoResponseDTO toResponse(ObjetoMuseo entity, LocalDate fechaIngreso, List<CategoriaObjetoResponseDTO> categorias) {
        return toResponse(entity, fechaIngreso, categorias, List.of(), null);
    }

    public static ObjetoMuseoResponseDTO toResponse(
            ObjetoMuseo entity,
            LocalDate fechaIngreso,
            List<CategoriaObjetoResponseDTO> categorias,
            List<FotoObjetoMuseoResponseDTO> fotos,
            ReciboEscaneadoObjetoMuseoResponseDTO reciboEscaneado
    ) {
        return new ObjetoMuseoResponseDTO(
                entity.getId(),
                entity.getNumeroInventario(),
                entity.getDenominacionObjeto(),
                entity.getDescripcion(),
                entity.getDescripcionTecnica(),
                entity.getMateriales(),
                entity.getDimensiones(),
                entity.getEstadoConservacion(),
                fechaIngreso,
                entity.getOrigenCarga(),
                entity.getDatosCompletos(),
                entity.getFechaCargaRapida(),
                entity.getCargaRapidaPor(),
                categorias,
                fotos,
                reciboEscaneado
        );
    }

    public static void updateEntity(ObjetoMuseo entity, ObjetoMuseoRequestDTO dto) {
        entity.setNumeroInventario(dto.numeroInventario());
        entity.setDenominacionObjeto(dto.denominacionObjeto());
        entity.setDescripcion(dto.descripcion());
        entity.setDescripcionTecnica(dto.descripcionTecnica());
        entity.setMateriales(dto.materiales());
        entity.setDimensiones(dto.dimensiones());
        entity.setEstadoConservacion(dto.estadoConservacion());
    }
}
