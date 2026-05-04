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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObjetoMuseoService {

    private static final Logger log = LoggerFactory.getLogger(ObjetoMuseoService.class);

    private final ObjetoMuseoRepository objetoMuseoRepository;

    public ObjetoMuseoService(ObjetoMuseoRepository objetoMuseoRepository) {
        this.objetoMuseoRepository = objetoMuseoRepository;
    }

    @Transactional
    public ObjetoMuseoResponseDTO crear(ObjetoMuseoRequestDTO dto) {
        validarNumeroInventarioDisponible(dto.numeroInventario(), null);
        ObjetoMuseo saved = objetoMuseoRepository.save(ObjetoMuseoMapper.toEntity(dto));
        log.info("event=objeto_museo.created objetoMuseoId={} numeroInventario={}", saved.getId(), saved.getNumeroInventario());
        return ObjetoMuseoMapper.toResponse(saved);
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
        ObjetoMuseo saved = objetoMuseoRepository.save(entity);
        log.info("event=objeto_museo.updated objetoMuseoId={} numeroInventario={}", saved.getId(), saved.getNumeroInventario());
        return ObjetoMuseoMapper.toResponse(saved);
    }

    @Transactional
    public void bajaLogica(Long id) {
        ObjetoMuseo entity = buscarActivo(id);
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(LocalDateTime.now());
        objetoMuseoRepository.save(entity);
        log.info("event=objeto_museo.deleted objetoMuseoId={} numeroInventario={}", entity.getId(), entity.getNumeroInventario());
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
                    log.warn("event=objeto_museo.business_error reason=numero_inventario_duplicado objetoMuseoId={} numeroInventario={}", objeto.getId(), numeroInventario);
                    throw new BusinessException("Ya existe un objeto con ese numero de inventario");
                });
    }
}
