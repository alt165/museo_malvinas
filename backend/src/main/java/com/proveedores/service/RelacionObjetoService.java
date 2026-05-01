package com.proveedores.service;

import com.proveedores.dto.RelacionObjetoRequestDTO;
import com.proveedores.dto.RelacionObjetoResponseDTO;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.RelacionObjeto;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.RelacionObjetoMapper;
import com.proveedores.repository.ObjetoMuseoRepository;
import com.proveedores.repository.RelacionObjetoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RelacionObjetoService {

    private final RelacionObjetoRepository relacionObjetoRepository;
    private final ObjetoMuseoRepository objetoMuseoRepository;

    public RelacionObjetoService(RelacionObjetoRepository relacionObjetoRepository, ObjetoMuseoRepository objetoMuseoRepository) {
        this.relacionObjetoRepository = relacionObjetoRepository;
        this.objetoMuseoRepository = objetoMuseoRepository;
    }

    @Transactional
    public RelacionObjetoResponseDTO crear(RelacionObjetoRequestDTO dto) {
        RelacionObjeto entity = RelacionObjetoMapper.toEntity(dto);
        entity.setObjetoOrigen(buscarObjeto(dto.objetoOrigenId()));
        entity.setObjetoDestino(buscarObjeto(dto.objetoDestinoId()));
        return RelacionObjetoMapper.toResponse(relacionObjetoRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public RelacionObjetoResponseDTO obtenerPorId(Long id) {
        return RelacionObjetoMapper.toResponse(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<RelacionObjetoResponseDTO> listar() {
        return relacionObjetoRepository.findAll().stream().filter(e -> !e.getEliminado()).map(RelacionObjetoMapper::toResponse).toList();
    }

    @Transactional
    public RelacionObjetoResponseDTO actualizar(Long id, RelacionObjetoRequestDTO dto) {
        RelacionObjeto entity = buscarActivo(id);
        entity.setObjetoOrigen(buscarObjeto(dto.objetoOrigenId()));
        entity.setObjetoDestino(buscarObjeto(dto.objetoDestinoId()));
        entity.setTipoRelacion(dto.tipoRelacion());
        entity.setDescripcion(dto.descripcion());
        return RelacionObjetoMapper.toResponse(relacionObjetoRepository.save(entity));
    }

    @Transactional
    public void bajaLogica(Long id) {
        RelacionObjeto entity = buscarActivo(id);
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(LocalDateTime.now());
        relacionObjetoRepository.save(entity);
    }

    private RelacionObjeto buscarActivo(Long id) {
        RelacionObjeto entity = relacionObjetoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Relacion entre objetos no encontrada"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Relacion entre objetos no encontrada");
        }
        return entity;
    }

    private ObjetoMuseo buscarObjeto(Long id) {
        ObjetoMuseo entity = objetoMuseoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Objeto de museo no encontrado"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Objeto de museo no encontrado");
        }
        return entity;
    }
}
