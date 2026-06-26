package com.proveedores.dto;

import com.proveedores.entity.CaracterRecepcionObjeto;
import com.proveedores.entity.DetalleEstadoConservacion;
import com.proveedores.entity.EstadoConservacion;
import com.proveedores.entity.EstadoIntegridad;
import com.proveedores.entity.HumedadConservacion;
import com.proveedores.entity.IntervencionesInadecuadas;
import com.proveedores.entity.OrigenCargaObjeto;
import com.proveedores.entity.RegimenPropiedad;
import com.proveedores.entity.VisibilidadCampo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ObjetoMuseoResponseDTO(
        Long id,
        String numeroInventario,
        String denominacionObjeto,
        String descripcion,
        String descripcionTecnica,
        String materiales,
        String alto,
        String ancho,
        String diametro,
        String espesor,
        String peso,
        String inscripciones,
        RegimenPropiedad regimenPropiedad,
        String condicionLegalBien,
        EstadoConservacion estadoConservacion,
        Set<DetalleEstadoConservacion> detallesEstadoConservacion,
        IntervencionesInadecuadas intervencionesInadecuadas,
        EstadoIntegridad estadoIntegridad,
        HumedadConservacion humedadConservacion,
        String temperaturaConservacion,
        String luzConservacion,
        Boolean conservacionExtintores,
        Boolean conservacionMontaje,
        Boolean conservacionSistemaElectrico,
        Boolean conservacionAlarmas,
        Boolean conservacionCamaras,
        Map<String, VisibilidadCampo> visibilidades,
        LocalDate fechaIngreso,
        OrigenCargaObjeto origenCarga,
        Boolean datosCompletos,
        LocalDateTime fechaCargaRapida,
        String cargaRapidaPor,
        Long ubicacionId,
        String ubicacionNombre,
        Long coleccionId,
        String coleccionNombre,
        Long depositanteId,
        String depositanteNombre,
        CaracterRecepcionObjeto caracterRecepcion,
        LocalDate fechaVencimiento,
        List<CategoriaObjetoResponseDTO> categorias,
        List<FotoObjetoMuseoResponseDTO> fotos,
        ReciboEscaneadoObjetoMuseoResponseDTO reciboEscaneado
) {
    public ObjetoMuseoResponseDTO(
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
        this(
                id, numeroInventario, denominacionObjeto, descripcion, descripcionTecnica, materiales,
                dimensiones, null, null, null, null, null, null, null, estadoConservacion, Set.of(),
                null, null, null, null, null, null, null, null, null, null, Map.of(),
                fechaIngreso, origenCarga, datosCompletos, fechaCargaRapida, cargaRapidaPor,
                null, null, null, null, null, null, null, null, categorias, fotos, reciboEscaneado
        );
    }

    public String dimensiones() {
        return Stream.of(alto, ancho, diametro, espesor, peso)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining("; "));
    }
}
