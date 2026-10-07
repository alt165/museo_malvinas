package com.proveedores.service;

import com.proveedores.dto.ExhibicionObjetoRequestDTO;
import com.proveedores.dto.ExhibicionObjetoResponseDTO;
import com.proveedores.entity.EstadoExhibicion;
import com.proveedores.entity.EstadoExhibicionObjeto;
import com.proveedores.entity.Exhibicion;
import com.proveedores.entity.ExhibicionObjeto;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.TipoOperacionAuditoria;
import com.proveedores.entity.Usuario;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ConflictException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.ExhibicionObjetoMapper;
import com.proveedores.repository.ExhibicionObjetoRepository;
import com.proveedores.repository.ExhibicionRepository;
import com.proveedores.repository.ObjetoMuseoRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExhibicionObjetoService {

    private static final Logger log = LoggerFactory.getLogger(ExhibicionObjetoService.class);
    private static final LocalDate FECHA_INFINITA = LocalDate.of(9999, 12, 31);

    private final ExhibicionObjetoRepository exhibicionObjetoRepository;
    private final ExhibicionRepository exhibicionRepository;
    private final ObjetoMuseoRepository objetoMuseoRepository;
    private final UsuarioMovimientoService usuarioMovimientoService;
    private final AuditoriaObjetoService auditoriaObjetoService;

    public ExhibicionObjetoService(ExhibicionObjetoRepository exhibicionObjetoRepository, ExhibicionRepository exhibicionRepository, ObjetoMuseoRepository objetoMuseoRepository, UsuarioMovimientoService usuarioMovimientoService, AuditoriaObjetoService auditoriaObjetoService) {
        this.exhibicionObjetoRepository = exhibicionObjetoRepository;
        this.exhibicionRepository = exhibicionRepository;
        this.objetoMuseoRepository = objetoMuseoRepository;
        this.usuarioMovimientoService = usuarioMovimientoService;
        this.auditoriaObjetoService = auditoriaObjetoService;
    }

    @Transactional
    public ExhibicionObjetoResponseDTO crear(ExhibicionObjetoRequestDTO dto) {
        return crear(dto, null);
    }

    @Transactional
    public ExhibicionObjetoResponseDTO crear(ExhibicionObjetoRequestDTO dto, String operador) {
        Exhibicion exhibicion = buscarExhibicion(dto.exhibicionId());
        validarEstadoAsignable(exhibicion);
        bloquearYValidarAsignaciones(Set.of(dto.objetoMuseoId()), exhibicion.getFechaInicio(), exhibicion.getFechaFin(), exhibicion.getId());
        ObjetoMuseo objeto = buscarObjeto(dto.objetoMuseoId());
        ExhibicionObjeto entity = exhibicionObjetoRepository
                .findByExhibicionIdAndObjetoMuseoId(dto.exhibicionId(), dto.objetoMuseoId())
                .orElse(null);
        if (entity != null && !entity.getEliminado()) {
            throw new ConflictException("El objeto ya esta asociado a esta exhibicion");
        }
        if (entity == null) entity = new ExhibicionObjeto();
        inicializarAsignacion(entity, exhibicion, objeto);
        ExhibicionObjeto saved = exhibicionObjetoRepository.save(entity);
        auditoriaObjetoService.registrar(
                objeto,
                TipoOperacionAuditoria.MODIFICACION,
                "INCORPORACION_EXHIBICION",
                "Incorporación del objeto a exhibición",
                "EXHIBICION",
                null,
                snapshotExhibicion(saved),
                operador
        );
        log.info("event=exhibicion_objeto.created exhibicionObjetoId={} exhibicionId={} objetoMuseoId={} estado={}", saved.getId(), exhibicion.getId(), objeto.getId(), saved.getEstado());
        return ExhibicionObjetoMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ExhibicionObjetoResponseDTO obtenerPorId(Long id) {
        return ExhibicionObjetoMapper.toResponse(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<ExhibicionObjetoResponseDTO> listar() {
        return exhibicionObjetoRepository.findAll().stream().filter(e -> !e.getEliminado()).map(ExhibicionObjetoMapper::toResponse).toList();
    }

    @Transactional
    public ExhibicionObjetoResponseDTO verificarDevolucion(Long id, String observaciones, String operador) {
        ExhibicionObjeto entity = buscarActivo(id);
        bloquearObjetos(List.of(entity.getObjetoMuseo().getId()));
        if (Boolean.TRUE.equals(entity.getDevolucionVerificada())) {
            throw new ConflictException("La devolución ya fue verificada");
        }
        Usuario verificador = usuarioMovimientoService.resolver(operador)
                .orElseThrow(() -> new BusinessException("No se pudo identificar al usuario que verifica la devolución"));
        var anteriores = snapshotExhibicion(entity);
        entity.setEstado(EstadoExhibicionObjeto.DEVUELTO);
        entity.setDevolucionVerificada(true);
        entity.setVerificadoPor(verificador);
        entity.setFechaVerificacion(com.proveedores.time.MuseoTime.now());
        entity.setFechaRetiro(com.proveedores.time.MuseoTime.today());
        entity.setObservacionesDevolucion(observaciones);
        ExhibicionObjeto saved = exhibicionObjetoRepository.save(entity);
        auditoriaObjetoService.registrar(
                saved.getObjetoMuseo(),
                TipoOperacionAuditoria.MODIFICACION,
                "VERIFICACION_DEVOLUCION",
                "Verificación de devolución del objeto en exhibición",
                "EXHIBICION",
                anteriores,
                snapshotExhibicion(saved),
                operador
        );
        log.info("event=exhibicion_objeto.return_verified exhibicionObjetoId={} exhibicionId={} objetoMuseoId={}", saved.getId(), saved.getExhibicion().getId(), saved.getObjetoMuseo().getId());
        return ExhibicionObjetoMapper.toResponse(saved);
    }

    @Transactional
    public ExhibicionObjetoResponseDTO revertirDevolucion(Long id, String operador) {
        ExhibicionObjeto entity = buscarActivo(id);
        bloquearObjetos(List.of(entity.getObjetoMuseo().getId()));
        var anteriores = snapshotExhibicion(entity);
        entity.setEstado(entity.getExhibicion().getEstado() == EstadoExhibicion.FINALIZADA
                ? EstadoExhibicionObjeto.PENDIENTE_REVISION
                : EstadoExhibicionObjeto.EN_EXHIBICION);
        entity.setDevolucionVerificada(false);
        entity.setVerificadoPor(null);
        entity.setFechaVerificacion(null);
        entity.setFechaRetiro(null);
        ExhibicionObjeto saved = exhibicionObjetoRepository.save(entity);
        auditoriaObjetoService.registrar(
                saved.getObjetoMuseo(),
                TipoOperacionAuditoria.MODIFICACION,
                "REVERSION_DEVOLUCION",
                "Reversión de verificación de devolución del objeto",
                "EXHIBICION",
                anteriores,
                snapshotExhibicion(saved),
                operador
        );
        log.info("event=exhibicion_objeto.return_reverted exhibicionObjetoId={} exhibicionId={} objetoMuseoId={}", saved.getId(), saved.getExhibicion().getId(), saved.getObjetoMuseo().getId());
        return ExhibicionObjetoMapper.toResponse(saved);
    }

    @Transactional
    public void bajaLogica(Long id, String operador) {
        ExhibicionObjeto entity = buscarActivo(id);
        bloquearObjetos(List.of(entity.getObjetoMuseo().getId()));
        eliminarAsignacion(entity, operador);
        log.info("event=exhibicion_objeto.deleted exhibicionObjetoId={} exhibicionId={} objetoMuseoId={}", entity.getId(), entity.getExhibicion().getId(), entity.getObjetoMuseo().getId());
    }


    private java.util.Map<String, Object> snapshotExhibicion(ExhibicionObjeto entity) {
        return auditoriaObjetoService.mapOf(
                "exhibicionObjetoId", entity.getId(),
                "exhibicionId", entity.getExhibicion() == null ? null : entity.getExhibicion().getId(),
                "exhibicion", entity.getExhibicion() == null ? null : entity.getExhibicion().getNombre(),
                "estado", entity.getEstado(),
                "fechaInclusion", entity.getFechaInclusion(),
                "fechaRetiro", entity.getFechaRetiro(),
                "devolucionVerificada", entity.getDevolucionVerificada(),
                "fechaVerificacion", entity.getFechaVerificacion(),
                "observacionesDevolucion", entity.getObservacionesDevolucion()
        );
    }

    public void bloquearYValidarAsignaciones(Set<Long> objetoIds, LocalDate fechaInicio, LocalDate fechaFin, Long exhibicionId) {
        if (objetoIds == null || objetoIds.isEmpty()) {
            return;
        }
        Set<Long> ids = idsUnicos(objetoIds);
        bloquearObjetos(ids);
        List<String> conflictos = new ArrayList<>();
        for (Long objetoId : ids) {
            ObjetoMuseo objeto = buscarObjeto(objetoId);
            Exhibicion conflicto = buscarConflicto(objetoId, fechaInicio, fechaFin, exhibicionId);
            if (conflicto != null) {
                conflictos.add("El objeto " + objeto.getNumeroInventario() + " - " + objeto.getDenominacionObjeto()
                        + " no puede permanecer en esta exhibición porque también está incluido en '"
                        + conflicto.getNombre() + "' en un rango de fechas coincidente o tiene devolución pendiente.");
            }
        }
        if (!conflictos.isEmpty()) {
            throw new ConflictException(String.join(" ", conflictos));
        }
    }

    public void bloquearObjetos(Collection<Long> objetoIds) {
        List<Long> idsOrdenados = objetoIds.stream().filter(Objects::nonNull).distinct().sorted().toList();
        if (!idsOrdenados.isEmpty()) {
            objetoMuseoRepository.lockIdsForUpdate(idsOrdenados);
        }
    }

    public void sincronizarObjetos(Exhibicion exhibicion, Set<Long> objetoIds, String operador) {
        if (objetoIds == null) {
            return;
        }
        Set<Long> ids = idsUnicos(objetoIds);
        List<ExhibicionObjeto> existentes = exhibicionObjetoRepository.findByExhibicionIdAndEliminadoFalse(exhibicion.getId());
        Map<Long, ExhibicionObjeto> porObjeto = existentes.stream()
                .collect(Collectors.toMap(item -> item.getObjetoMuseo().getId(), Function.identity()));
        for (ExhibicionObjeto existente : existentes) {
            if (!ids.contains(existente.getObjetoMuseo().getId())) {
                eliminarAsignacion(existente, operador);
            }
        }
        for (Long objetoId : ids) {
            if (porObjeto.containsKey(objetoId)) {
                continue;
            }
            ObjetoMuseo objeto = buscarObjeto(objetoId);
            ExhibicionObjeto relacion = exhibicionObjetoRepository
                    .findByExhibicionIdAndObjetoMuseoId(exhibicion.getId(), objetoId)
                    .orElseGet(ExhibicionObjeto::new);
            inicializarAsignacion(relacion, exhibicion, objeto);
            ExhibicionObjeto saved = exhibicionObjetoRepository.save(relacion);
            auditoriaObjetoService.registrar(objeto, TipoOperacionAuditoria.MODIFICACION, "INCORPORACION_EXHIBICION", "Incorporación del objeto a exhibición", "EXHIBICION", null, snapshotExhibicion(saved), operador);
        }
    }

    public Exhibicion buscarConflicto(Long objetoId, LocalDate fechaInicio, LocalDate fechaFin, Long exhibicionId) {
        return exhibicionObjetoRepository.findByObjetoMuseoIdAndEliminadoFalse(objetoId).stream()
                .filter(relacion -> relacion.getExhibicion() != null && !relacion.getExhibicion().getEliminado())
                .filter(relacion -> exhibicionId == null || !relacion.getExhibicion().getId().equals(exhibicionId))
                .filter(this::bloqueaDisponibilidad)
                .filter(relacion -> {
                    Exhibicion exhibicion = relacion.getExhibicion();
                    LocalDate finBloqueo = exhibicion.getEstado() == EstadoExhibicion.FINALIZADA
                            ? FECHA_INFINITA : exhibicion.getFechaFin();
                    return haySuperposicion(fechaInicio, fechaFin, exhibicion.getFechaInicio(), finBloqueo);
                })
                .map(ExhibicionObjeto::getExhibicion)
                .findFirst()
                .orElse(null);
    }

    private boolean bloqueaDisponibilidad(ExhibicionObjeto relacion) {
        if (Boolean.TRUE.equals(relacion.getDevolucionVerificada()) && relacion.getEstado() == EstadoExhibicionObjeto.DEVUELTO) {
            return false;
        }
        EstadoExhibicion estado = relacion.getExhibicion().getEstado();
        return estado == EstadoExhibicion.PLANIFICADA || estado == EstadoExhibicion.ACTIVA || estado == EstadoExhibicion.FINALIZADA;
    }

    private boolean haySuperposicion(LocalDate inicioA, LocalDate finA, LocalDate inicioB, LocalDate finB) {
        LocalDate finNormalA = finA == null ? FECHA_INFINITA : finA;
        LocalDate finNormalB = finB == null ? FECHA_INFINITA : finB;
        return !inicioA.isAfter(finNormalB) && !finNormalA.isBefore(inicioB);
    }

    private void validarEstadoAsignable(Exhibicion exhibicion) {
        if (exhibicion.getEstado() == EstadoExhibicion.FINALIZADA || exhibicion.getEstado() == EstadoExhibicion.CANCELADA) {
            throw new ConflictException("No se pueden asociar objetos a una exhibición finalizada o cancelada");
        }
    }

    private void inicializarAsignacion(ExhibicionObjeto relacion, Exhibicion exhibicion, ObjetoMuseo objeto) {
        relacion.setActivo(true);
        relacion.setEliminado(false);
        relacion.setFechaEliminacion(null);
        relacion.setExhibicion(exhibicion);
        relacion.setObjetoMuseo(objeto);
        relacion.setFechaInclusion(exhibicion.getFechaInicio());
        relacion.setFechaRetiro(null);
        relacion.setEstado(EstadoExhibicionObjeto.EN_EXHIBICION);
        relacion.setDevolucionVerificada(false);
        relacion.setVerificadoPor(null);
        relacion.setFechaVerificacion(null);
        relacion.setObservacionesDevolucion(null);
    }

    private void eliminarAsignacion(ExhibicionObjeto entity, String operador) {
        EstadoExhibicion estadoExhibicion = entity.getExhibicion().getEstado();
        boolean devolucionConfirmada = Boolean.TRUE.equals(entity.getDevolucionVerificada())
                && entity.getEstado() == EstadoExhibicionObjeto.DEVUELTO;
        if ((estadoExhibicion == EstadoExhibicion.ACTIVA || estadoExhibicion == EstadoExhibicion.FINALIZADA)
                && !devolucionConfirmada) {
            throw new ConflictException("No se puede liberar un objeto de una exhibición iniciada sin verificar su devolución");
        }
        var anteriores = snapshotExhibicion(entity);
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(com.proveedores.time.MuseoTime.now());
        exhibicionObjetoRepository.save(entity);
        auditoriaObjetoService.registrar(entity.getObjetoMuseo(), TipoOperacionAuditoria.MODIFICACION, "REMOCION_EXHIBICION", "Remoción del objeto de exhibición", "EXHIBICION", anteriores, null, operador);
    }

    private Set<Long> idsUnicos(Set<Long> objetoIds) {
        Set<Long> ids = objetoIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.size() != objetoIds.size()) {
            throw new BusinessException("No se permiten objetos duplicados o inválidos");
        }
        return ids;
    }

    private ExhibicionObjeto buscarActivo(Long id) {
        ExhibicionObjeto entity = exhibicionObjetoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Objeto de exhibicion no encontrado"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Objeto de exhibicion no encontrado");
        }
        return entity;
    }

    private Exhibicion buscarExhibicion(Long id) {
        Exhibicion entity = exhibicionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Exhibicion no encontrada"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Exhibicion no encontrada");
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
