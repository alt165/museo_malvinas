package com.proveedores.service;

import com.proveedores.dto.RangoMilitarRequestDTO;
import com.proveedores.dto.RangoMilitarResponseDTO;
import com.proveedores.entity.Fuerza;
import com.proveedores.entity.RangoMilitar;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.RangoMilitarRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RangoMilitarService {

    private final RangoMilitarRepository rangoMilitarRepository;

    public RangoMilitarService(RangoMilitarRepository rangoMilitarRepository) {
        this.rangoMilitarRepository = rangoMilitarRepository;
    }

    @Transactional
    public RangoMilitarResponseDTO crear(RangoMilitarRequestDTO dto) {
        RangoMilitar rango = new RangoMilitar();
        aplicar(rango, dto);
        return toResponse(rangoMilitarRepository.save(rango));
    }

    @Transactional(readOnly = true)
    public List<RangoMilitarResponseDTO> listar() {
        return rangoMilitarRepository.findByActivoTrueAndEliminadoFalseOrderByFuerzaAscOrdenJerarquicoAscNombreAsc()
                .stream()
                .map(this::toResponse)
                .toList();
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

    @Transactional
    public RangoMilitarResponseDTO actualizar(Long id, RangoMilitarRequestDTO dto) {
        RangoMilitar rango = buscarActivo(id);
        aplicar(rango, dto);
        return toResponse(rangoMilitarRepository.save(rango));
    }

    @Transactional
    public void bajaLogica(Long id) {
        RangoMilitar rango = buscarActivo(id);
        rango.setActivo(false);
        rango.setEliminado(true);
        rango.setFechaEliminacion(LocalDateTime.now());
        rangoMilitarRepository.save(rango);
    }

    RangoMilitar buscarActivo(Long id) {
        RangoMilitar rango = rangoMilitarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rango militar no encontrado"));
        if (!Boolean.TRUE.equals(rango.getActivo()) || Boolean.TRUE.equals(rango.getEliminado())) {
            throw new ResourceNotFoundException("Rango militar no encontrado");
        }
        return rango;
    }

    private void aplicar(RangoMilitar rango, RangoMilitarRequestDTO dto) {
        rango.setFuerza(dto.fuerza());
        rango.setNombre(dto.nombre().trim());
        rango.setOrdenJerarquico(dto.ordenJerarquico());
    }

    private RangoMilitarResponseDTO toResponse(RangoMilitar rango) {
        return new RangoMilitarResponseDTO(rango.getId(), rango.getFuerza(), rango.getNombre(), rango.getOrdenJerarquico());
    }
}
