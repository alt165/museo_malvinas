package com.proveedores.service;

import com.proveedores.dto.AgregarObjetosColeccionRequestDTO;
import com.proveedores.dto.ColeccionObjetoRequestDTO;
import com.proveedores.dto.ColeccionObjetoResponseDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.entity.ColeccionObjeto;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.TipoOperacionAuditoria;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.ColeccionObjetoMapper;
import com.proveedores.repository.ColeccionObjetoRepository;
import com.proveedores.repository.ObjetoMuseoRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ColeccionObjetoService {

    private final ColeccionObjetoRepository coleccionObjetoRepository;
    private final ObjetoMuseoRepository objetoMuseoRepository;
    private final ObjetoMuseoService objetoMuseoService;
    private final AuditoriaObjetoService auditoriaObjetoService;

    public ColeccionObjetoService(
            ColeccionObjetoRepository coleccionObjetoRepository,
            ObjetoMuseoRepository objetoMuseoRepository,
            ObjetoMuseoService objetoMuseoService,
            AuditoriaObjetoService auditoriaObjetoService
    ) {
        this.coleccionObjetoRepository = coleccionObjetoRepository;
        this.objetoMuseoRepository = objetoMuseoRepository;
        this.objetoMuseoService = objetoMuseoService;
        this.auditoriaObjetoService = auditoriaObjetoService;
    }

    @Transactional
    public ColeccionObjetoResponseDTO crear(ColeccionObjetoRequestDTO dto) {
        return crear(dto, null);
    }

    @Transactional
    public ColeccionObjetoResponseDTO crear(ColeccionObjetoRequestDTO dto, String operador) {
        validarNombreDisponible(dto.nombre(), null);
        ColeccionObjeto saved = coleccionObjetoRepository.save(ColeccionObjetoMapper.toEntity(dto));
        sincronizarObjetos(saved, dto.objetoIds(), operador);
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
        return actualizar(id, dto, null);
    }

    @Transactional
    public ColeccionObjetoResponseDTO actualizar(Long id, ColeccionObjetoRequestDTO dto, String operador) {
        ColeccionObjeto entity = buscarActiva(id);
        validarNombreDisponible(dto.nombre(), id);
        entity.setNombre(dto.nombre());
        entity.setDescripcion(dto.descripcion());
        ColeccionObjeto saved = coleccionObjetoRepository.save(entity);
        sincronizarObjetos(saved, dto.objetoIds(), operador);
        return toResponse(saved);
    }

    @Transactional
    public void bajaLogica(Long id, String operador) {
        ColeccionObjeto entity = buscarActiva(id);
        List<ObjetoMuseo> objetosAsociados = objetoMuseoRepository.findByColeccionObjetoIdAndEliminadoFalseOrderByNumeroInventarioAsc(id);
        objetosAsociados.forEach(objeto -> {
            objeto.setColeccionObjeto(null);
            objetoMuseoRepository.save(objeto);
            registrarDesvinculacionPorEliminacionColeccion(objeto, entity, operador);
        });
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(LocalDateTime.now());
        coleccionObjetoRepository.save(entity);
        log.info("event=coleccion_objeto.deleted coleccionId={} objetosDesvinculados={}", entity.getId(), objetosAsociados.size());
    }

    @Transactional(readOnly = true)
    public List<ObjetoMuseoResponseDTO> listarObjetos(Long id) {
        buscarActiva(id);
        return objetoMuseoRepository.findByColeccionObjetoIdAndEliminadoFalseOrderByNumeroInventarioAsc(id).stream()
                .map(objetoMuseoService::toResponseForRelations)
                .toList();
    }

    @Transactional
    public List<ObjetoMuseoResponseDTO> agregarObjetos(Long id, AgregarObjetosColeccionRequestDTO dto, String operador) {
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
            if (!Boolean.TRUE.equals(objeto.getActivo())) {
                throw new ResourceNotFoundException("Objeto de museo no encontrado");
            }
            if (objeto.getColeccionObjeto() != null) {
                throw new BusinessException("El objeto " + objeto.getNumeroInventario() + " ya pertenece a una coleccion");
            }
            objeto.setColeccionObjeto(coleccion);
            objetoMuseoRepository.save(objeto);
            registrarIncorporacionColeccion(objeto, coleccion, operador);
        }
        return listarObjetos(id);
    }

    @Transactional
    public void quitarObjeto(Long id, Long objetoId, String operador) {
        buscarActiva(id);
        ObjetoMuseo objeto = buscarObjetoActivo(objetoId);
        if (objeto.getColeccionObjeto() == null || !objeto.getColeccionObjeto().getId().equals(id)) {
            throw new ResourceNotFoundException("Objeto de la coleccion no encontrado");
        }
        ColeccionObjeto coleccionAnterior = objeto.getColeccionObjeto();
        objeto.setColeccionObjeto(null);
        objetoMuseoRepository.save(objeto);
        registrarDesvinculacionColeccion(objeto, coleccionAnterior, operador);
    }

    private void sincronizarObjetos(ColeccionObjeto coleccion, List<Long> objetoIds, String operador) {
        if (objetoIds == null) {
            return;
        }

        List<Long> idsNormalizados = objetoIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (idsNormalizados.size() != objetoIds.stream().filter(java.util.Objects::nonNull).count()) {
            throw new BusinessException("No se permiten objetos duplicados");
        }

        Set<Long> idsFinales = new LinkedHashSet<>(idsNormalizados);
        List<ObjetoMuseo> objetosActuales = objetoMuseoRepository.findByColeccionObjetoIdAndEliminadoFalseOrderByNumeroInventarioAsc(coleccion.getId());
        for (ObjetoMuseo objetoActual : objetosActuales) {
            if (!idsFinales.contains(objetoActual.getId())) {
                objetoActual.setColeccionObjeto(null);
                objetoMuseoRepository.save(objetoActual);
                registrarDesvinculacionColeccion(objetoActual, coleccion, operador);
            }
        }

        for (Long objetoId : idsFinales) {
            ObjetoMuseo objeto = buscarObjetoActivo(objetoId);
            if (!Boolean.TRUE.equals(objeto.getActivo())) {
                throw new ResourceNotFoundException("Objeto de museo no encontrado");
            }
            if (objeto.getColeccionObjeto() != null && !objeto.getColeccionObjeto().getId().equals(coleccion.getId())) {
                throw new BusinessException("El objeto " + objeto.getNumeroInventario() + " ya pertenece a otra coleccion");
            }
            if (objeto.getColeccionObjeto() == null) {
                objeto.setColeccionObjeto(coleccion);
                objetoMuseoRepository.save(objeto);
                registrarIncorporacionColeccion(objeto, coleccion, operador);
            }
        }
    }

    private void registrarIncorporacionColeccion(ObjetoMuseo objeto, ColeccionObjeto coleccion, String operador) {
        auditoriaObjetoService.registrar(
                objeto,
                TipoOperacionAuditoria.MODIFICACION,
                "INCORPORACION_COLECCION",
                "El objeto fue incorporado a la colección: " + coleccion.getNombre() + ".",
                "COLECCION",
                auditoriaObjetoService.mapOf("coleccionId", null, "coleccion", null),
                valoresColeccion(coleccion),
                operador
        );
    }

    private void registrarDesvinculacionColeccion(ObjetoMuseo objeto, ColeccionObjeto coleccionAnterior, String operador) {
        auditoriaObjetoService.registrar(
                objeto,
                TipoOperacionAuditoria.MODIFICACION,
                "DESVINCULACION_COLECCION",
                "El objeto fue desvinculado de la colección: " + coleccionAnterior.getNombre() + ".",
                "COLECCION",
                valoresColeccion(coleccionAnterior),
                auditoriaObjetoService.mapOf("coleccionId", null, "coleccion", null),
                operador
        );
    }

    private void registrarDesvinculacionPorEliminacionColeccion(ObjetoMuseo objeto, ColeccionObjeto coleccionAnterior, String operador) {
        auditoriaObjetoService.registrar(
                objeto,
                TipoOperacionAuditoria.MODIFICACION,
                "DESVINCULACION_POR_ELIMINACION_COLECCION",
                "El objeto quedó sin colección por eliminación de la colección: " + coleccionAnterior.getNombre() + ".",
                "ELIMINACION_COLECCION",
                valoresColeccion(coleccionAnterior),
                auditoriaObjetoService.mapOf("coleccionId", null, "coleccion", null),
                operador
        );
    }

    private Map<String, Object> valoresColeccion(ColeccionObjeto coleccion) {
        return auditoriaObjetoService.mapOf(
                "coleccionId", coleccion == null ? null : coleccion.getId(),
                "coleccion", coleccion == null ? null : coleccion.getNombre()
        );
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
