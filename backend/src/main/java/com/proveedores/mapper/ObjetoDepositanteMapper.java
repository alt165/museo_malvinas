package com.proveedores.mapper;

import com.proveedores.dto.ObjetoDepositanteRequestDTO;
import com.proveedores.dto.ObjetoDepositanteResponseDTO;
import com.proveedores.entity.ObjetoDepositante;

public final class ObjetoDepositanteMapper {
    private ObjetoDepositanteMapper() {
    }

    public static ObjetoDepositante toEntity(ObjetoDepositanteRequestDTO dto) {
        ObjetoDepositante entity = new ObjetoDepositante();
        entity.setObjetoMuseo(MapperReferences.objetoMuseo(dto.objetoMuseoId()));
        entity.setDepositante(MapperReferences.depositante(dto.depositanteId()));
        entity.setFechaDeposito(dto.fechaDeposito());
        entity.setTipoDeposito(dto.tipoDeposito());
        entity.setObservaciones(dto.observaciones());
        return entity;
    }

    public static ObjetoDepositanteResponseDTO toResponse(ObjetoDepositante entity) {
        return new ObjetoDepositanteResponseDTO(entity.getId(), entity.getObjetoMuseo().getId(), entity.getObjetoMuseo().getDenominacionObjeto(), entity.getDepositante().getId(), entity.getDepositante().getNombre(), entity.getFechaDeposito(), entity.getTipoDeposito(), entity.getObservaciones());
    }
}
