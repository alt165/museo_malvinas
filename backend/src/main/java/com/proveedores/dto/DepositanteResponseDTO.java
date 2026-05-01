package com.proveedores.dto;

import com.proveedores.entity.TipoDepositante;

public record DepositanteResponseDTO(Long id, String nombre, TipoDepositante tipo, String contacto, String observaciones) {
}
