package com.proveedores.service;

import com.proveedores.dto.DetalleConservacionRequestDTO;
import com.proveedores.dto.DetalleConservacionResponseDTO;
import com.proveedores.entity.DetalleConservacion;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.DetalleConservacionRepository;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DetalleConservacionService {

    private static final Pattern NO_ASCII = Pattern.compile("[^A-Z0-9_]");
    private final DetalleConservacionRepository detalleConservacionRepository;

    public DetalleConservacionService(DetalleConservacionRepository detalleConservacionRepository) {
        this.detalleConservacionRepository = detalleConservacionRepository;
    }

    @Transactional
    public DetalleConservacionResponseDTO crear(DetalleConservacionRequestDTO dto) {
        DetalleConservacion detalle = new DetalleConservacion();
        aplicar(detalle, dto);
        return toResponse(detalleConservacionRepository.save(detalle));
    }

    @Transactional(readOnly = true)
    public List<DetalleConservacionResponseDTO> listar() {
        return detalleConservacionRepository.findByActivoTrueAndEliminadoFalseOrderByNombreAsc()
                .stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(DetalleConservacionResponseDTO::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public DetalleConservacionResponseDTO obtenerPorId(Long id) {
        return toResponse(buscarActivo(id));
    }

    @Transactional
    public DetalleConservacionResponseDTO actualizar(Long id, DetalleConservacionRequestDTO dto) {
        DetalleConservacion detalle = buscarActivo(id);
        aplicar(detalle, dto);
        return toResponse(detalleConservacionRepository.save(detalle));
    }

    @Transactional
    public void bajaLogica(Long id) {
        DetalleConservacion detalle = buscarActivo(id);
        detalle.setActivo(false);
        detalle.setEliminado(true);
        detalle.setFechaEliminacion(LocalDateTime.now());
        detalleConservacionRepository.save(detalle);
    }

    @Transactional(readOnly = true)
    public Set<DetalleConservacion> buscarActivosPorCodigos(Set<String> codigos) {
        if (codigos == null || codigos.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<String> normalizados = new LinkedHashSet<>(codigos.stream()
                .filter(codigo -> codigo != null && !codigo.isBlank())
                .map(codigo -> codigo.trim().toUpperCase(Locale.ROOT))
                .toList());
        List<DetalleConservacion> encontrados = detalleConservacionRepository.findByCodigoInAndActivoTrueAndEliminadoFalse(normalizados);
        if (encontrados.size() != normalizados.size()) {
            throw new ResourceNotFoundException("Detalle de conservacion no encontrado");
        }
        return new LinkedHashSet<>(encontrados.stream()
                .sorted(Comparator.comparing(DetalleConservacion::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList());
    }

    private DetalleConservacion buscarActivo(Long id) {
        DetalleConservacion detalle = detalleConservacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Detalle de conservacion no encontrado"));
        if (!Boolean.TRUE.equals(detalle.getActivo()) || Boolean.TRUE.equals(detalle.getEliminado())) {
            throw new ResourceNotFoundException("Detalle de conservacion no encontrado");
        }
        return detalle;
    }

    private void aplicar(DetalleConservacion detalle, DetalleConservacionRequestDTO dto) {
        detalle.setNombre(dto.nombre().trim());
        detalle.setCodigo(normalizarCodigo(dto.codigo(), dto.nombre()));
        detalle.setDescripcion(normalizar(dto.descripcion()));
    }

    private String normalizar(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizarCodigo(String codigo, String nombre) {
        String base = codigo == null || codigo.isBlank() ? nombre : codigo;
        String sinAcentos = Normalizer.normalize(base.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String candidate = sinAcentos.toUpperCase(Locale.ROOT).replaceAll("\\s+", "_");
        candidate = NO_ASCII.matcher(candidate).replaceAll("").replaceAll("_+", "_");
        return candidate.replaceAll("^_+|_+$", "");
    }

    private DetalleConservacionResponseDTO toResponse(DetalleConservacion detalle) {
        return new DetalleConservacionResponseDTO(detalle.getId(), detalle.getCodigo(), detalle.getNombre(), detalle.getDescripcion());
    }
}
