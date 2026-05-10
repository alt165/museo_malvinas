package com.proveedores.service;

import com.proveedores.dto.ObjetoDigitalRequestDTO;
import com.proveedores.dto.ObjetoDigitalResponseDTO;
import com.proveedores.entity.ObjetoDigital;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.ObjetoDigitalMapper;
import com.proveedores.repository.ObjetoDigitalRepository;
import com.proveedores.repository.ObjetoMuseoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObjetoDigitalService {

    private final ObjetoDigitalRepository objetoDigitalRepository;
    private final ObjetoMuseoRepository objetoMuseoRepository;

    public ObjetoDigitalService(ObjetoDigitalRepository objetoDigitalRepository, ObjetoMuseoRepository objetoMuseoRepository) {
        this.objetoDigitalRepository = objetoDigitalRepository;
        this.objetoMuseoRepository = objetoMuseoRepository;
    }

    @Transactional
    public ObjetoDigitalResponseDTO crear(ObjetoDigitalRequestDTO dto) {
        validarNumeroInventarioDisponible(dto.numeroInventario(), null);
        return ObjetoDigitalMapper.toResponse(objetoDigitalRepository.save(ObjetoDigitalMapper.toEntity(dto)));
    }

    @Transactional(readOnly = true)
    public ObjetoDigitalResponseDTO obtenerPorId(Long id) {
        return ObjetoDigitalMapper.toResponse(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<ObjetoDigitalResponseDTO> listar() {
        return objetoDigitalRepository.findAll().stream().filter(e -> !e.getEliminado()).map(ObjetoDigitalMapper::toResponse).toList();
    }

    @Transactional
    public ObjetoDigitalResponseDTO actualizar(Long id, ObjetoDigitalRequestDTO dto) {
        ObjetoDigital entity = buscarActivo(id);
        validarNumeroInventarioDisponible(dto.numeroInventario(), id);
        entity.setNumeroInventario(dto.numeroInventario());
        entity.setDenominacionObjeto(dto.denominacionObjeto());
        entity.setDescripcion(dto.descripcion());
        entity.setFormatoDigital(dto.formatoDigital());
        entity.setIdentificadorDigital(dto.identificadorDigital());
        entity.setMetadatos(dto.metadatos());
        return ObjetoDigitalMapper.toResponse(objetoDigitalRepository.save(entity));
    }

    @Transactional
    public void bajaLogica(Long id) {
        ObjetoDigital entity = buscarActivo(id);
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(LocalDateTime.now());
        objetoDigitalRepository.save(entity);
    }

    private ObjetoDigital buscarActivo(Long id) {
        ObjetoDigital entity = objetoDigitalRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Objeto digital no encontrado"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Objeto digital no encontrado");
        }
        return entity;
    }

    private void validarNumeroInventarioDisponible(String numeroInventario, Long idActual) {
        objetoMuseoRepository.findByNumeroInventario(numeroInventario)
                .filter(objeto -> !objeto.getEliminado())
                .filter(objeto -> idActual == null || !objeto.getId().equals(idActual))
                .ifPresent(objeto -> { throw new BusinessException("Ya existe un objeto con ese numero de inventario"); });
    }
}
