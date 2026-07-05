package com.proveedores.service;

import com.proveedores.dto.UnidadMilitarRequestDTO;
import com.proveedores.dto.UnidadMilitarResponseDTO;
import com.proveedores.entity.Fuerza;
import com.proveedores.entity.UnidadMilitar;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.UnidadMilitarRepository;
import java.time.LocalDateTime;
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

    @Transactional
    public UnidadMilitarResponseDTO crear(UnidadMilitarRequestDTO dto) {
        UnidadMilitar unidad = new UnidadMilitar();
        aplicar(unidad, dto);
        return toResponse(unidadMilitarRepository.save(unidad));
    }

    @Transactional(readOnly = true)
    public List<UnidadMilitarResponseDTO> listar() {
        return unidadMilitarRepository.findByActivoTrueAndEliminadoFalseOrderByFuerzaAscNombreAsc()
                .stream()
                .map(this::toResponse)
                .toList();
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

    @Transactional
    public UnidadMilitarResponseDTO actualizar(Long id, UnidadMilitarRequestDTO dto) {
        UnidadMilitar unidad = buscarActivo(id);
        aplicar(unidad, dto);
        return toResponse(unidadMilitarRepository.save(unidad));
    }

    @Transactional
    public void bajaLogica(Long id) {
        UnidadMilitar unidad = buscarActivo(id);
        unidad.setActivo(false);
        unidad.setEliminado(true);
        unidad.setFechaEliminacion(LocalDateTime.now());
        unidadMilitarRepository.save(unidad);
    }

    UnidadMilitar buscarActivo(Long id) {
        UnidadMilitar unidad = unidadMilitarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad militar no encontrada"));
        if (!Boolean.TRUE.equals(unidad.getActivo()) || Boolean.TRUE.equals(unidad.getEliminado())) {
            throw new ResourceNotFoundException("Unidad militar no encontrada");
        }
        return unidad;
    }

    private void aplicar(UnidadMilitar unidad, UnidadMilitarRequestDTO dto) {
        unidad.setFuerza(dto.fuerza());
        unidad.setNombre(dto.nombre().trim());
        unidad.setSigla(normalizar(dto.sigla()));
        unidad.setTipoUnidad(normalizar(dto.tipoUnidad()));
        unidad.setDescripcion(normalizar(dto.descripcion()));
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
