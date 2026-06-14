package com.proveedores.service;

import com.proveedores.dto.UnidadMilitarResponseDTO;
import com.proveedores.entity.Fuerza;
import com.proveedores.entity.UnidadMilitar;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.UnidadMilitarRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnidadMilitarService {

    private final UnidadMilitarRepository unidadMilitarRepository;

    public UnidadMilitarService(UnidadMilitarRepository unidadMilitarRepository) {
        this.unidadMilitarRepository = unidadMilitarRepository;
    }

    @Transactional(readOnly = true)
    public List<UnidadMilitarResponseDTO> buscarPorFuerza(Fuerza fuerza, String buscar, Integer limite) {
        int size = Math.min(Math.max(limite == null ? 20 : limite, 1), 50);
        String filtro = normalizar(buscar);
        return unidadMilitarRepository.buscarActivasPorFuerza(fuerza, filtro, PageRequest.of(0, size))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UnidadMilitarResponseDTO obtenerPorId(Long id) {
        return toResponse(buscarActivo(id));
    }

    UnidadMilitar buscarActivo(Long id) {
        UnidadMilitar unidad = unidadMilitarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad militar no encontrada"));
        if (!Boolean.TRUE.equals(unidad.getActivo()) || Boolean.TRUE.equals(unidad.getEliminado())) {
            throw new ResourceNotFoundException("Unidad militar no encontrada");
        }
        return unidad;
    }

    private String normalizar(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private UnidadMilitarResponseDTO toResponse(UnidadMilitar unidad) {
        return new UnidadMilitarResponseDTO(unidad.getId(), unidad.getFuerza(), unidad.getNombre(), unidad.getSigla(), unidad.getTipoUnidad(), unidad.getDescripcion());
    }
}
