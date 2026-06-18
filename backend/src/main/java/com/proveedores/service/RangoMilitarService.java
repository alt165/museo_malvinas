package com.proveedores.service;

import com.proveedores.dto.RangoMilitarResponseDTO;
import com.proveedores.entity.Fuerza;
import com.proveedores.entity.RangoMilitar;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.RangoMilitarRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RangoMilitarService {

    private final RangoMilitarRepository rangoMilitarRepository;

    public RangoMilitarService(RangoMilitarRepository rangoMilitarRepository) {
        this.rangoMilitarRepository = rangoMilitarRepository;
    }

    @Transactional(readOnly = true)
    public List<RangoMilitarResponseDTO> listarPorFuerza(Fuerza fuerza) {
        return rangoMilitarRepository.findByFuerzaAndActivoTrueAndEliminadoFalseOrderByOrdenJerarquicoAsc(fuerza)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RangoMilitarResponseDTO obtenerPorId(Long id) {
        return toResponse(buscarActivo(id));
    }

    RangoMilitar buscarActivo(Long id) {
        RangoMilitar rango = rangoMilitarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rango militar no encontrado"));
        if (!Boolean.TRUE.equals(rango.getActivo()) || Boolean.TRUE.equals(rango.getEliminado())) {
            throw new ResourceNotFoundException("Rango militar no encontrado");
        }
        return rango;
    }

    private RangoMilitarResponseDTO toResponse(RangoMilitar rango) {
        return new RangoMilitarResponseDTO(rango.getId(), rango.getFuerza(), rango.getNombre(), rango.getOrdenJerarquico());
    }
}
