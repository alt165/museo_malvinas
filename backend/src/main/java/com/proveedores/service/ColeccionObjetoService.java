package com.proveedores.service;

import com.proveedores.dto.AgregarObjetosColeccionRequestDTO;
import com.proveedores.dto.ColeccionObjetoRequestDTO;
import com.proveedores.dto.ColeccionObjetoResponseDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.entity.ColeccionObjeto;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.ColeccionObjetoMapper;
import com.proveedores.repository.ColeccionObjetoRepository;
import com.proveedores.repository.ObjetoMuseoRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ColeccionObjetoService {

    private final ColeccionObjetoRepository coleccionObjetoRepository;
    private final ObjetoMuseoRepository objetoMuseoRepository;
    private final ObjetoMuseoService objetoMuseoService;

    public ColeccionObjetoService(
            ColeccionObjetoRepository coleccionObjetoRepository,
            ObjetoMuseoRepository objetoMuseoRepository,
            ObjetoMuseoService objetoMuseoService
    ) {
        this.coleccionObjetoRepository = coleccionObjetoRepository;
        this.objetoMuseoRepository = objetoMuseoRepository;
        this.objetoMuseoService = objetoMuseoService;
    }

    @Transactional
    public ColeccionObjetoResponseDTO crear(ColeccionObjetoRequestDTO dto) {
        validarNombreDisponible(dto.nombre(), null);
        ColeccionObjeto saved = coleccionObjetoRepository.save(ColeccionObjetoMapper.toEntity(dto));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ColeccionObjetoResponseDTO> listar() {
        return coleccionObjetoRepository.findAll().stream()
                .filter(coleccion -> !coleccion.getEliminado())
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ColeccionObjetoResponseDTO obtenerPorId(Long id) {
        return toResponse(buscarActiva(id));
    }

    @Transactional
    public ColeccionObjetoResponseDTO actualizar(Long id, ColeccionObjetoRequestDTO dto) {
        ColeccionObjeto entity = buscarActiva(id);
        validarNombreDisponible(dto.nombre(), id);
        entity.setNombre(dto.nombre());
        entity.setDescripcion(dto.descripcion());
        return toResponse(coleccionObjetoRepository.save(entity));
    }

    @Transactional
    public void bajaLogica(Long id) {
        ColeccionObjeto entity = buscarActiva(id);
        objetoMuseoRepository.findByColeccionObjetoIdAndEliminadoFalseOrderByNumeroInventarioAsc(id)
                .forEach(objeto -> {
                    objeto.setColeccionObjeto(null);
                    objetoMuseoRepository.save(objeto);
                });
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(LocalDateTime.now());
        coleccionObjetoRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<ObjetoMuseoResponseDTO> listarObjetos(Long id) {
        buscarActiva(id);
        return objetoMuseoRepository.findByColeccionObjetoIdAndEliminadoFalseOrderByNumeroInventarioAsc(id).stream()
                .map(objetoMuseoService::toResponseForRelations)
                .toList();
    }

    @Transactional
    public List<ObjetoMuseoResponseDTO> agregarObjetos(Long id, AgregarObjetosColeccionRequestDTO dto) {
        ColeccionObjeto coleccion = buscarActiva(id);
        List<Long> objetoIds = dto.objetoIds().stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (objetoIds.isEmpty()) {
            throw new BusinessException("Debe seleccionar al menos un objeto");
        }
        if (new HashSet<>(objetoIds).size() != dto.objetoIds().stream().filter(java.util.Objects::nonNull).count()) {
            throw new BusinessException("No se permiten objetos duplicados");
        }

        for (Long objetoId : objetoIds) {
            ObjetoMuseo objeto = buscarObjetoActivo(objetoId);
            if (objeto.getColeccionObjeto() != null) {
                throw new BusinessException("El objeto " + objeto.getNumeroInventario() + " ya pertenece a una coleccion");
            }
            objeto.setColeccionObjeto(coleccion);
            objetoMuseoRepository.save(objeto);
        }
        return listarObjetos(id);
    }

    @Transactional
    public void quitarObjeto(Long id, Long objetoId) {
        buscarActiva(id);
        ObjetoMuseo objeto = buscarObjetoActivo(objetoId);
        if (objeto.getColeccionObjeto() == null || !objeto.getColeccionObjeto().getId().equals(id)) {
            throw new ResourceNotFoundException("Objeto de la coleccion no encontrado");
        }
        objeto.setColeccionObjeto(null);
        objetoMuseoRepository.save(objeto);
    }

    private ColeccionObjeto buscarActiva(Long id) {
        ColeccionObjeto entity = coleccionObjetoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coleccion no encontrada"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Coleccion no encontrada");
        }
        return entity;
    }

    private ObjetoMuseo buscarObjetoActivo(Long id) {
        ObjetoMuseo objeto = objetoMuseoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Objeto de museo no encontrado"));
        if (objeto.getEliminado()) {
            throw new ResourceNotFoundException("Objeto de museo no encontrado");
        }
        return objeto;
    }

    private void validarNombreDisponible(String nombre, Long idActual) {
        coleccionObjetoRepository.findByNombreIgnoreCaseAndEliminadoFalse(nombre)
                .filter(coleccion -> idActual == null || !coleccion.getId().equals(idActual))
                .ifPresent(coleccion -> {
                    throw new BusinessException("Ya existe una coleccion con ese nombre");
                });
    }

    private ColeccionObjetoResponseDTO toResponse(ColeccionObjeto entity) {
        return ColeccionObjetoMapper.toResponse(entity, objetoMuseoRepository.countByColeccionObjetoIdAndEliminadoFalse(entity.getId()));
    }
}
