package com.proveedores.service;

import com.proveedores.dto.DepositanteRequestDTO;
import com.proveedores.dto.DepositanteResponseDTO;
import com.proveedores.entity.Depositante;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.DepositanteMapper;
import com.proveedores.repository.DepositanteRepository;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepositanteService {

    private final DepositanteRepository depositanteRepository;

    public DepositanteService(DepositanteRepository depositanteRepository) {
        this.depositanteRepository = depositanteRepository;
    }

    @Transactional
    public DepositanteResponseDTO crear(DepositanteRequestDTO dto) {
        return DepositanteMapper.toResponse(depositanteRepository.save(DepositanteMapper.toEntity(dto)));
    }

    @Transactional(readOnly = true)
    public DepositanteResponseDTO obtenerPorId(Long id) {
        return DepositanteMapper.toResponse(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<DepositanteResponseDTO> listar() {
        return depositanteRepository.findAll().stream().filter(e -> !e.getEliminado()).map(DepositanteMapper::toResponse).toList();
    }

    @Transactional
    public DepositanteResponseDTO actualizar(Long id, DepositanteRequestDTO dto) {
        Depositante entity = buscarActivo(id);
        entity.setNombre(dto.nombre());
        entity.setTipo(dto.tipo());
        entity.setContacto(dto.contacto());
        entity.setDni(dto.dni());
        entity.setCuit(dto.cuit());
        entity.setObservaciones(dto.observaciones());
        return DepositanteMapper.toResponse(depositanteRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public DepositanteResponseDTO buscarPorIdentificacion(String valor) {
        String identificacion = normalizarIdentificacion(valor);
        return depositanteRepository.findActivoByIdentificacionNormalizada(identificacion)
                .map(DepositanteMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Depositante no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<DepositanteResponseDTO> buscarPorNombre(String valor) {
        String nombre = normalizarBusquedaNombre(valor);
        String nombreNormalizado = normalizarTexto(nombre);
        LinkedHashMap<Long, Depositante> resultados = new LinkedHashMap<>();

        depositanteRepository.findByNombreContainingIgnoreCaseAndEliminadoFalse(nombre)
                .forEach(depositante -> resultados.put(depositante.getId(), depositante));

        depositanteRepository.findAll().stream()
                .filter(depositante -> !depositante.getEliminado())
                .filter(depositante -> normalizarTexto(depositante.getNombre()).contains(nombreNormalizado))
                .forEach(depositante -> resultados.putIfAbsent(depositante.getId(), depositante));

        return resultados.values().stream().map(DepositanteMapper::toResponse).toList();
    }

    @Transactional
    public void bajaLogica(Long id) {
        Depositante entity = buscarActivo(id);
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(LocalDateTime.now());
        depositanteRepository.save(entity);
    }

    private Depositante buscarActivo(Long id) {
        Depositante entity = depositanteRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Depositante no encontrado"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Depositante no encontrado");
        }
        return entity;
    }

    private String normalizarIdentificacion(String valor) {
        if (valor == null) {
            throw new BusinessException("La identificacion es obligatoria");
        }
        String normalizado = valor.replace(".", "").replace("-", "").replace(" ", "").trim();
        if (normalizado.isBlank()) {
            throw new BusinessException("La identificacion es obligatoria");
        }
        return normalizado;
    }

    private String normalizarBusquedaNombre(String valor) {
        if (valor == null || valor.trim().isBlank()) {
            throw new BusinessException("El nombre de busqueda es obligatorio");
        }

        return valor.trim();
    }

    private String normalizarTexto(String valor) {
        String sinAcentos = Normalizer.normalize(valor == null ? "" : valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return sinAcentos.toLowerCase();
    }
}
