package com.proveedores.service;

import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.ObjetoMuseoMapper;
import com.proveedores.repository.ObjetoMuseoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObjetoMuseoService {

    private final ObjetoMuseoRepository objetoMuseoRepository;

    public ObjetoMuseoService(ObjetoMuseoRepository objetoMuseoRepository) {
        this.objetoMuseoRepository = objetoMuseoRepository;
    }

    @Transactional
    public ObjetoMuseoResponseDTO crear(ObjetoMuseoRequestDTO dto) {
        validarNumeroInventarioDisponible(dto.numeroInventario(), null);
        return ObjetoMuseoMapper.toResponse(objetoMuseoRepository.save(ObjetoMuseoMapper.toEntity(dto)));
    }

    @Transactional(readOnly = true)
    public ObjetoMuseoResponseDTO obtenerPorId(Long id) {
        return ObjetoMuseoMapper.toResponse(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<ObjetoMuseoResponseDTO> listar() {
        return objetoMuseoRepository.findAll().stream()
                .filter(objeto -> !objeto.getEliminado())
                .map(ObjetoMuseoMapper::toResponse)
                .toList();
    }

    @Transactional
    public ObjetoMuseoResponseDTO actualizar(Long id, ObjetoMuseoRequestDTO dto) {
        ObjetoMuseo entity = buscarActivo(id);
        validarNumeroInventarioDisponible(dto.numeroInventario(), id);
        entity.setNumeroInventario(dto.numeroInventario());
        entity.setNombre(dto.nombre());
        entity.setTipoObjeto(dto.tipoObjeto());
        entity.setDescripcion(dto.descripcion());
        return ObjetoMuseoMapper.toResponse(objetoMuseoRepository.save(entity));
    }

    @Transactional
    public void bajaLogica(Long id) {
        ObjetoMuseo entity = buscarActivo(id);
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(LocalDateTime.now());
        objetoMuseoRepository.save(entity);
    }

    private ObjetoMuseo buscarActivo(Long id) {
        ObjetoMuseo entity = objetoMuseoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Objeto de museo no encontrado"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Objeto de museo no encontrado");
        }
        return entity;
    }

    private void validarNumeroInventarioDisponible(String numeroInventario, Long idActual) {
        objetoMuseoRepository.findByNumeroInventario(numeroInventario)
                .filter(objeto -> !objeto.getEliminado())
                .filter(objeto -> idActual == null || !objeto.getId().equals(idActual))
                .ifPresent(objeto -> {
                    throw new BusinessException("Ya existe un objeto con ese numero de inventario");
                });
    }
}
