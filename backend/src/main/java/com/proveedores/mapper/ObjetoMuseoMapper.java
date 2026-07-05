package com.proveedores.mapper;

import com.proveedores.dto.CategoriaObjetoResponseDTO;
import com.proveedores.dto.FotoObjetoMuseoResponseDTO;
import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.dto.ReciboEscaneadoObjetoMuseoResponseDTO;
import com.proveedores.entity.CaracterRecepcionObjeto;
import com.proveedores.entity.DetalleConservacion;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.VisibilidadCampo;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        return toResponse(entity, null, null, null, null, null, null, null, null, null, categorias, List.of(), null);
    }

    public static ObjetoMuseoResponseDTO toResponse(ObjetoMuseo entity, LocalDate fechaIngreso, List<CategoriaObjetoResponseDTO> categorias) {
        return toResponse(entity, fechaIngreso, null, null, null, null, null, null, null, null, categorias, List.of(), null);
    }

    public static ObjetoMuseoResponseDTO toResponse(
            ObjetoMuseo entity,
            LocalDate fechaIngreso,
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
        return new ObjetoMuseoResponseDTO(
                entity.getId(),
                entity.getNumeroInventario(),
                entity.getDenominacionObjeto(),
                entity.getDescripcion(),
                entity.getDescripcionTecnica(),
                entity.getMateriales(),
                entity.getAlto(),
                entity.getAncho(),
                entity.getDiametro(),
                entity.getEspesor(),
                entity.getPeso(),
                entity.getInscripciones(),
                entity.getRegimenPropiedad(),
                entity.getCondicionLegalBien(),
                entity.getEstadoConservacion(),
                entity.getDetallesEstadoConservacion() == null ? Set.of() : entity.getDetallesEstadoConservacion().stream()
                        .sorted(Comparator.comparing(DetalleConservacion::getNombre, String.CASE_INSENSITIVE_ORDER))
                        .map(DetalleConservacion::getCodigo)
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)),
                entity.getIntervencionesInadecuadas(),
                entity.getEstadoIntegridad(),
                entity.getHumedadConservacion(),
                entity.getTemperaturaConservacion(),
                entity.getLuzConservacion(),
                entity.getConservacionExtintores(),
                entity.getConservacionMontaje(),
                entity.getConservacionSistemaElectrico(),
                entity.getConservacionAlarmas(),
                entity.getConservacionCamaras(),
                entity.getVisibilidades() == null ? Map.of() : Map.copyOf(entity.getVisibilidades()),
                fechaIngreso,
                entity.getOrigenCarga(),
                entity.getDatosCompletos(),
                entity.getFechaCargaRapida(),
                entity.getCargaRapidaPor(),
                ubicacionId,
                ubicacionNombre,
                coleccionId,
                coleccionNombre,
                depositanteId,
                depositanteNombre,
                caracterRecepcion,
                fechaVencimiento,
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
        entity.setAlto(dto.alto());
        entity.setAncho(dto.ancho());
        entity.setDiametro(dto.diametro());
        entity.setEspesor(dto.espesor());
        entity.setPeso(dto.peso());
        entity.setInscripciones(dto.inscripciones());
        entity.setRegimenPropiedad(dto.regimenPropiedad());
        entity.setCondicionLegalBien(dto.condicionLegalBien());
        entity.setEstadoConservacion(dto.estadoConservacion());
        entity.setIntervencionesInadecuadas(dto.intervencionesInadecuadas());
        entity.setEstadoIntegridad(dto.estadoIntegridad());
        entity.setHumedadConservacion(dto.humedadConservacion());
        entity.setTemperaturaConservacion(dto.temperaturaConservacion());
        entity.setLuzConservacion(dto.luzConservacion());
        entity.setConservacionExtintores(dto.conservacionExtintores());
        entity.setConservacionMontaje(dto.conservacionMontaje());
        entity.setConservacionSistemaElectrico(dto.conservacionSistemaElectrico());
        entity.setConservacionAlarmas(dto.conservacionAlarmas());
        entity.setConservacionCamaras(dto.conservacionCamaras());
        entity.setVisibilidades(normalizarVisibilidades(dto.visibilidades()));
    }

    private static Map<String, VisibilidadCampo> normalizarVisibilidades(Map<String, VisibilidadCampo> visibilidades) {
        Map<String, VisibilidadCampo> normalizadas = new LinkedHashMap<>();
        if (visibilidades == null) {
            return normalizadas;
        }
        visibilidades.forEach((campo, visibilidad) -> {
            if (campo != null && !campo.isBlank() && visibilidad != null && visibilidad != VisibilidadCampo.PUBLICO) {
                normalizadas.put(campo.trim(), visibilidad);
            }
        });
        return normalizadas;
    }
}
