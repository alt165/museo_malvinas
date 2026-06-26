package com.proveedores.dto;

import com.proveedores.entity.CaracterRecepcionObjeto;
import com.proveedores.entity.DetalleEstadoConservacion;
import com.proveedores.entity.EstadoConservacion;
import com.proveedores.entity.EstadoIntegridad;
import com.proveedores.entity.HumedadConservacion;
import com.proveedores.entity.IntervencionesInadecuadas;
import com.proveedores.entity.RegimenPropiedad;
import com.proveedores.entity.VisibilidadCampo;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public record ObjetoMuseoRequestDTO(
        @NotBlank(message = "El numero de inventario es obligatorio")
        @Size(max = 80, message = "El numero de inventario no puede superar 80 caracteres")
        String numeroInventario,

        @Size(max = 160, message = "La denominacion no puede superar 160 caracteres")
        String denominacionObjeto,

        String descripcion,

        String descripcionTecnica,

        String materiales,

        @Size(max = 80, message = "El alto no puede superar 80 caracteres")
        String alto,

        @Size(max = 80, message = "El ancho no puede superar 80 caracteres")
        String ancho,

        @Size(max = 80, message = "El diametro no puede superar 80 caracteres")
        String diametro,

        @Size(max = 80, message = "El espesor no puede superar 80 caracteres")
        String espesor,

        @Size(max = 80, message = "El peso no puede superar 80 caracteres")
        String peso,

        @Size(max = 500, message = "Las inscripciones no pueden superar 500 caracteres")
        String inscripciones,

        RegimenPropiedad regimenPropiedad,

        String condicionLegalBien,

        EstadoConservacion estadoConservacion,

        Set<DetalleEstadoConservacion> detallesEstadoConservacion,

        IntervencionesInadecuadas intervencionesInadecuadas,

        EstadoIntegridad estadoIntegridad,

        HumedadConservacion humedadConservacion,

        @Size(max = 80, message = "La temperatura no puede superar 80 caracteres")
        String temperaturaConservacion,

        @Size(max = 80, message = "La luz no puede superar 80 caracteres")
        String luzConservacion,

        Boolean conservacionExtintores,

        Boolean conservacionMontaje,

        Boolean conservacionSistemaElectrico,

        Boolean conservacionAlarmas,

        Boolean conservacionCamaras,

        Map<String, VisibilidadCampo> visibilidades,

        Set<Long> categoriaIds,

        Long ubicacionId,

        Long depositanteId,

        CaracterRecepcionObjeto caracterRecepcion,

        LocalDate fechaVencimiento
) {
    public ObjetoMuseoRequestDTO(
            String numeroInventario,
            String denominacionObjeto,
            String descripcion,
            String descripcionTecnica,
            String materiales,
            String dimensiones,
            EstadoConservacion estadoConservacion,
            Set<Long> categoriaIds
    ) {
        this(numeroInventario, denominacionObjeto, descripcion, descripcionTecnica, materiales, dimensiones, null, null, null, null, null, null, null, estadoConservacion, null, null, null, null, null, null, null, null, null, null, null, null, categoriaIds, null, null, null, null);
    }

    public ObjetoMuseoRequestDTO(
            String numeroInventario,
            String denominacionObjeto,
            String descripcion,
            String descripcionTecnica,
            String materiales,
            String dimensiones,
            EstadoConservacion estadoConservacion,
            Set<Long> categoriaIds,
            Long ubicacionId
    ) {
        this(numeroInventario, denominacionObjeto, descripcion, descripcionTecnica, materiales, dimensiones, null, null, null, null, null, null, null, estadoConservacion, null, null, null, null, null, null, null, null, null, null, null, null, categoriaIds, ubicacionId, null, null, null);
    }


    public ObjetoMuseoRequestDTO(
            String numeroInventario,
            String denominacionObjeto,
            String descripcion,
            String descripcionTecnica,
            String materiales,
            String dimensiones,
            EstadoConservacion estadoConservacion,
            Set<Long> categoriaIds,
            Long ubicacionId,
            Long depositanteId,
            CaracterRecepcionObjeto caracterRecepcion,
            LocalDate fechaVencimiento
    ) {
        this(numeroInventario, denominacionObjeto, descripcion, descripcionTecnica, materiales, dimensiones, null, null, null, null, null, null, null, estadoConservacion, null, null, null, null, null, null, null, null, null, null, null, null, categoriaIds, ubicacionId, depositanteId, caracterRecepcion, fechaVencimiento);
    }

    public String dimensiones() {
        return alto;
    }

    @AssertTrue(message = "La denominacion o nombre es obligatorio")
    public boolean isDenominacionONombreValida() {
        return denominacionObjeto != null && !denominacionObjeto.isBlank();
    }
}
