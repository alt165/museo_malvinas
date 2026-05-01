package com.proveedores.mapper;

import com.proveedores.dto.ObjetoDigitalRequestDTO;
import com.proveedores.dto.ObjetoDigitalResponseDTO;
import com.proveedores.entity.ObjetoDigital;

public final class ObjetoDigitalMapper {
    private ObjetoDigitalMapper() {
    }

    public static ObjetoDigital toEntity(ObjetoDigitalRequestDTO dto) {
        ObjetoDigital entity = new ObjetoDigital();
        entity.setNumeroInventario(dto.numeroInventario());
        entity.setNombre(dto.nombre());
        entity.setTipoObjeto(dto.tipoObjeto());
        entity.setDescripcion(dto.descripcion());
        entity.setFormatoDigital(dto.formatoDigital());
        entity.setIdentificadorDigital(dto.identificadorDigital());
        entity.setMetadatos(dto.metadatos());
        return entity;
    }

    public static ObjetoDigitalResponseDTO toResponse(ObjetoDigital entity) {
        return new ObjetoDigitalResponseDTO(entity.getId(), entity.getNumeroInventario(), entity.getNombre(), entity.getTipoObjeto(), entity.getDescripcion(), entity.getFormatoDigital(), entity.getIdentificadorDigital(), entity.getMetadatos());
    }
}
