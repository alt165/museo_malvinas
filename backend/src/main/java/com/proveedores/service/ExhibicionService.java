package com.proveedores.service;

import com.proveedores.dto.ExhibicionObjetoResponseDTO;
import com.proveedores.dto.ExhibicionProximaInicioResponseDTO;
import com.proveedores.dto.ExhibicionRequestDTO;
import com.proveedores.dto.ExhibicionResponseDTO;
import com.proveedores.dto.ObjetoDisponibilidadExhibicionResponseDTO;
import com.proveedores.entity.EstadoExhibicion;
import com.proveedores.entity.EstadoExhibicionObjeto;
import com.proveedores.entity.Exhibicion;
import com.proveedores.entity.ExhibicionObjeto;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.TipoExhibicion;
import com.proveedores.entity.TipoOperacionAuditoria;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ConflictException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.ExhibicionMapper;
import com.proveedores.mapper.ExhibicionObjetoMapper;
import com.proveedores.repository.ExhibicionObjetoRepository;
import com.proveedores.repository.ExhibicionRepository;
import com.proveedores.repository.ObjetoMuseoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ExhibicionService {

    private static final Logger log = LoggerFactory.getLogger(ExhibicionService.class);
    private static final LocalDate FECHA_INFINITA = LocalDate.of(9999, 12, 31);

    private final ExhibicionRepository exhibicionRepository;
    private final ExhibicionObjetoRepository exhibicionObjetoRepository;
    private final ObjetoMuseoRepository objetoMuseoRepository;
    private final AuditoriaObjetoService auditoriaObjetoService;

    public ExhibicionService(ExhibicionRepository exhibicionRepository, ExhibicionObjetoRepository exhibicionObjetoRepository, ObjetoMuseoRepository objetoMuseoRepository, AuditoriaObjetoService auditoriaObjetoService) {
        this.exhibicionRepository = exhibicionRepository;
        this.exhibicionObjetoRepository = exhibicionObjetoRepository;
        this.objetoMuseoRepository = objetoMuseoRepository;
        this.auditoriaObjetoService = auditoriaObjetoService;
    }

    @Transactional
    public ExhibicionResponseDTO crear(ExhibicionRequestDTO dto) {
        validarFechasAlta(dto.fechaInicio(), dto.fechaFin());
        validarObjetosSinConflicto(dto.objetoIds(), dto.fechaInicio(), dto.fechaFin(), null);
        Exhibicion entity = ExhibicionMapper.toEntity(dto);
        normalizarTipo(entity);
        entity.setEstado(estadoInicial(entity.getFechaInicio()));
        Exhibicion saved = exhibicionRepository.save(entity);
        sincronizarObjetos(saved, dto.objetoIds(), null);
        log.info("event=exhibicion.created exhibicionId={} estado={} tipo={}", saved.getId(), saved.getEstado(), saved.getTipo());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ExhibicionResponseDTO obtenerPorId(Long id) {
        return toResponse(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<ExhibicionResponseDTO> listar() {
        return exhibicionRepository.findAll().stream().filter(e -> !e.getEliminado()).map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ExhibicionProximaInicioResponseDTO> listarProximasAIniciar() {
        LocalDate hoy = LocalDate.now();
        return exhibicionRepository.buscarProximasAIniciar(hoy, hoy.plusDays(15)).stream()
                .map(exhibicion -> new ExhibicionProximaInicioResponseDTO(
                        exhibicion.getId(),
                        exhibicion.getNombre(),
                        exhibicion.getFechaInicio(),
                        ChronoUnit.DAYS.between(hoy, exhibicion.getFechaInicio()),
                        exhibicion.getTipo(),
                        exhibicion.getFechaFin() == null
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ObjetoDisponibilidadExhibicionResponseDTO> buscarObjetosDisponibilidad(String texto, LocalDate fechaInicio, LocalDate fechaFin, Long exhibicionId, Pageable pageable) {
        validarFechas(fechaInicio, fechaFin);
        Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), Math.min(Math.max(pageable.getPageSize(), 1), 30), pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("numeroInventario"));
        String filtro = StringUtils.hasText(texto) ? texto.trim() : null;
        return objetoMuseoRepository.buscarParaDisponibilidadExhibicion(filtro, pageRequest)
                .map(objeto -> toDisponibilidad(objeto, fechaInicio, fechaFin, exhibicionId));
    }

    @Transactional(readOnly = true)
    public Page<ExhibicionResponseDTO> buscarFinalizadas(String texto, Pageable pageable) {
        Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), Math.min(Math.max(pageable.getPageSize(), 1), 20), pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("nombre"));
        if (!StringUtils.hasText(texto)) {
            return exhibicionRepository.findByEstadoAndEliminadoFalse(EstadoExhibicion.FINALIZADA, pageRequest).map(this::toResponse);
        }
        return exhibicionRepository.buscarFinalizadasPorTexto(texto.trim(), pageRequest).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ObjetoDisponibilidadExhibicionResponseDTO> obtenerObjetosParaRepetir(Long id, LocalDate fechaInicioNueva, LocalDate fechaFinNueva) {
        validarFechas(fechaInicioNueva, fechaFinNueva);
        Exhibicion exhibicion = buscarActivo(id);
        if (exhibicion.getEstado() != EstadoExhibicion.FINALIZADA) {
            throw new BusinessException("Solo se pueden repetir exhibiciones finalizadas");
        }
        return exhibicionObjetoRepository.findByExhibicionIdAndEliminadoFalse(id).stream()
                .map(ExhibicionObjeto::getObjetoMuseo)
                .filter(Objects::nonNull)
                .filter(objeto -> !objeto.getEliminado() && Boolean.TRUE.equals(objeto.getActivo()))
                .map(objeto -> toDisponibilidad(objeto, fechaInicioNueva, fechaFinNueva, null))
                .toList();
    }

    @Transactional
    public ExhibicionResponseDTO actualizar(Long id, ExhibicionRequestDTO dto) {
        validarFechas(dto.fechaInicio(), dto.fechaFin());
        Exhibicion entity = buscarActivo(id);
        if (entity.getEstado() == EstadoExhibicion.CANCELADA) {
            throw new BusinessException("No se puede editar una exhibicion cancelada");
        }
        if (dto.estado() == EstadoExhibicion.CANCELADA) {
            throw new BusinessException("Para cancelar una exhibición debe usar la acción de cancelación.");
        }
        Set<Long> objetoIds = dto.objetoIds() == null ? idsObjetosActuales(id) : idsUnicos(dto.objetoIds());
        validarObjetosSinConflicto(objetoIds, dto.fechaInicio(), dto.fechaFin(), id);
        LocalDate fechaInicioAnterior = entity.getFechaInicio();
        LocalDate fechaFinAnterior = entity.getFechaFin();
        entity.setNombre(dto.nombre());
        entity.setDescripcion(dto.descripcion());
        entity.setFechaInicio(dto.fechaInicio());
        entity.setFechaFin(dto.fechaFin());
        normalizarTipo(entity);
        entity.setEstado(dto.estado());
        Exhibicion saved = exhibicionRepository.save(entity);
        if (dto.objetoIds() != null) {
            sincronizarObjetos(saved, objetoIds, null);
        }
        if (!Objects.equals(fechaInicioAnterior, saved.getFechaInicio()) || !Objects.equals(fechaFinAnterior, saved.getFechaFin())) {
            registrarCambioPeriodo(saved, fechaInicioAnterior, fechaFinAnterior, null);
        }
        log.info("event=exhibicion.updated exhibicionId={} estado={}", saved.getId(), saved.getEstado());
        return toResponse(saved);
    }

    @Transactional
    public ExhibicionResponseDTO finalizar(Long id) {
        Exhibicion entity = buscarActivo(id);
        if (entity.getEstado() == EstadoExhibicion.CANCELADA) {
            throw new BusinessException("No se puede finalizar una exhibicion cancelada");
        }
        if (entity.getEstado() == EstadoExhibicion.FINALIZADA) {
            return toResponse(entity);
        }
        LocalDate hoy = LocalDate.now();
        boolean anticipada = entity.getFechaFin() == null || entity.getFechaFin().isAfter(hoy);
        if (anticipada) {
            entity.setFechaFin(hoy);
            normalizarTipo(entity);
        }
        entity.setEstado(EstadoExhibicion.FINALIZADA);
        Exhibicion saved = exhibicionRepository.save(entity);
        liberarObjetosPorFinalizacion(saved, anticipada, null);
        log.info("event=exhibicion.finalized exhibicionId={} anticipada={}", saved.getId(), anticipada);
        return toResponse(saved);
    }

    @Transactional
    public ExhibicionResponseDTO cancelar(Long id) {
        Exhibicion entity = buscarActivo(id);
        LocalDate hoy = LocalDate.now();
        if (entity.getEstado() != EstadoExhibicion.PLANIFICADA || !entity.getFechaInicio().isAfter(hoy)) {
            throw new BusinessException("No se puede cancelar una exhibición que ya inició.");
        }
        entity.setEstado(EstadoExhibicion.CANCELADA);
        Exhibicion saved = exhibicionRepository.save(entity);
        liberarObjetosPorCancelacion(saved, null);
        log.info("event=exhibicion.cancelled exhibicionId={}", saved.getId());
        return toResponse(saved);
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void iniciarExhibicionesPlanificadasVencidas() {
        LocalDate hoy = LocalDate.now();
        List<Exhibicion> exhibiciones = exhibicionRepository.findByEstadoAndEliminadoFalseAndFechaInicioLessThanEqual(EstadoExhibicion.PLANIFICADA, hoy);
        for (Exhibicion exhibicion : exhibiciones) {
            exhibicion.setEstado(EstadoExhibicion.ACTIVA);
            Exhibicion saved = exhibicionRepository.save(exhibicion);
            registrarInicioAutomatico(saved, null);
            log.info("event=exhibicion.auto_started exhibicionId={}", saved.getId());
        }
    }

    @Transactional
    public void bajaLogica(Long id) {
        Exhibicion entity = buscarActivo(id);
        if (entity.getEstado() == EstadoExhibicion.ACTIVA) {
            log.warn("event=exhibicion.business_error reason=baja_exhibicion_activa exhibicionId={}", id);
            throw new BusinessException("No se puede dar de baja una exhibicion activa");
        }
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(LocalDateTime.now());
        exhibicionRepository.save(entity);
        log.info("event=exhibicion.deleted exhibicionId={}", entity.getId());
    }

    private ExhibicionResponseDTO toResponse(Exhibicion exhibicion) {
        List<ExhibicionObjetoResponseDTO> objetos = exhibicionObjetoRepository.findByExhibicionIdAndEliminadoFalse(exhibicion.getId()).stream()
                .map(ExhibicionObjetoMapper::toResponse)
                .toList();
        return ExhibicionMapper.toResponse(exhibicion, objetos);
    }

    private ObjetoDisponibilidadExhibicionResponseDTO toDisponibilidad(ObjetoMuseo objeto, LocalDate fechaInicio, LocalDate fechaFin, Long exhibicionId) {
        Exhibicion conflicto = buscarConflicto(objeto.getId(), fechaInicio, fechaFin, exhibicionId);
        if (conflicto == null) {
            return new ObjetoDisponibilidadExhibicionResponseDTO(objeto.getId(), objeto.getNumeroInventario(), objeto.getDenominacionObjeto(), true, null, null, null, null, null, false);
        }
        return new ObjetoDisponibilidadExhibicionResponseDTO(
                objeto.getId(),
                objeto.getNumeroInventario(),
                objeto.getDenominacionObjeto(),
                false,
                "Incluido en otra exhibición con fechas superpuestas",
                conflicto.getId(),
                conflicto.getNombre(),
                conflicto.getFechaInicio(),
                conflicto.getFechaFin(),
                conflicto.getFechaFin() == null
        );
    }

    private void sincronizarObjetos(Exhibicion exhibicion, Set<Long> objetoIds, String operador) {
        if (objetoIds == null) {
            return;
        }
        Set<Long> ids = idsUnicos(objetoIds);
        List<ExhibicionObjeto> existentes = exhibicionObjetoRepository.findByExhibicionIdAndEliminadoFalse(exhibicion.getId());
        Map<Long, ExhibicionObjeto> porObjeto = existentes.stream().collect(Collectors.toMap(item -> item.getObjetoMuseo().getId(), Function.identity()));
        for (ExhibicionObjeto existente : existentes) {
            if (!ids.contains(existente.getObjetoMuseo().getId())) {
                var anteriores = snapshotExhibicion(existente);
                existente.setActivo(false);
                existente.setEliminado(true);
                existente.setFechaEliminacion(LocalDateTime.now());
                exhibicionObjetoRepository.save(existente);
                auditoriaObjetoService.registrar(existente.getObjetoMuseo(), TipoOperacionAuditoria.MODIFICACION, "REMOCION_EXHIBICION", "Remoción del objeto de exhibición", "EXHIBICION", anteriores, null, operador);
            }
        }
        for (Long objetoId : ids) {
            if (porObjeto.containsKey(objetoId)) {
                continue;
            }
            ObjetoMuseo objeto = buscarObjetoActivo(objetoId);
            ExhibicionObjeto relacion = new ExhibicionObjeto();
            relacion.setExhibicion(exhibicion);
            relacion.setObjetoMuseo(objeto);
            relacion.setFechaInclusion(exhibicion.getFechaInicio());
            relacion.setFechaRetiro(exhibicion.getFechaFin());
            relacion.setEstado(EstadoExhibicionObjeto.EN_EXHIBICION);
            relacion.setDevolucionVerificada(false);
            ExhibicionObjeto saved = exhibicionObjetoRepository.save(relacion);
            auditoriaObjetoService.registrar(objeto, TipoOperacionAuditoria.MODIFICACION, "INCORPORACION_EXHIBICION", "Incorporación del objeto a exhibición", "EXHIBICION", null, snapshotExhibicion(saved), operador);
        }
    }

    private void registrarCambioPeriodo(Exhibicion exhibicion, LocalDate fechaInicioAnterior, LocalDate fechaFinAnterior, String operador) {
        for (ExhibicionObjeto relacion : exhibicionObjetoRepository.findByExhibicionIdAndEliminadoFalse(exhibicion.getId())) {
            auditoriaObjetoService.registrar(
                    relacion.getObjetoMuseo(),
                    TipoOperacionAuditoria.MODIFICACION,
                    "CAMBIO_PERIODO_EXHIBICION",
                    "Cambio del período de exhibición que afecta al objeto",
                    "EXHIBICION",
                    auditoriaObjetoService.mapOf("fechaInicio", fechaInicioAnterior, "fechaFin", fechaFinAnterior),
                    auditoriaObjetoService.mapOf("fechaInicio", exhibicion.getFechaInicio(), "fechaFin", exhibicion.getFechaFin(), "exhibicion", exhibicion.getNombre()),
                    operador
            );
        }
    }

    private void validarObjetosSinConflicto(Set<Long> objetoIds, LocalDate fechaInicio, LocalDate fechaFin, Long exhibicionId) {
        if (objetoIds == null || objetoIds.isEmpty()) {
            return;
        }
        Set<Long> ids = idsUnicos(objetoIds);
        List<String> conflictos = new ArrayList<>();
        for (Long objetoId : ids) {
            ObjetoMuseo objeto = buscarObjetoActivo(objetoId);
            Exhibicion conflicto = buscarConflicto(objetoId, fechaInicio, fechaFin, exhibicionId);
            if (conflicto != null) {
                conflictos.add("El objeto " + objeto.getNumeroInventario() + " - " + objeto.getDenominacionObjeto() + " no puede permanecer en esta exhibición porque también está incluido en '" + conflicto.getNombre() + "' en un rango de fechas coincidente.");
            }
        }
        if (!conflictos.isEmpty()) {
            throw new ConflictException(String.join(" ", conflictos));
        }
    }

    private Exhibicion buscarConflicto(Long objetoId, LocalDate fechaInicio, LocalDate fechaFin, Long exhibicionId) {
        return exhibicionObjetoRepository.findByObjetoMuseoIdAndEliminadoFalse(objetoId).stream()
                .map(ExhibicionObjeto::getExhibicion)
                .filter(exhibicion -> exhibicion != null && !exhibicion.getEliminado())
                .filter(exhibicion -> exhibicionId == null || !exhibicion.getId().equals(exhibicionId))
                .filter(this::bloqueaDisponibilidad)
                .filter(exhibicion -> haySuperposicion(fechaInicio, fechaFin, exhibicion.getFechaInicio(), fechaFinEfectiva(exhibicion)))
                .findFirst()
                .orElse(null);
    }

    private boolean haySuperposicion(LocalDate inicioA, LocalDate finA, LocalDate inicioB, LocalDate finB) {
        LocalDate finNormalA = finA == null ? FECHA_INFINITA : finA;
        LocalDate finNormalB = finB == null ? FECHA_INFINITA : finB;
        return !inicioA.isAfter(finNormalB) && !finNormalA.isBefore(inicioB);
    }

    private void validarFechasAlta(LocalDate fechaInicio, LocalDate fechaFin) {
        validarFechas(fechaInicio, fechaFin);
        if (fechaInicio.isBefore(LocalDate.now())) {
            throw new BusinessException("La fecha de inicio no puede ser anterior a la fecha actual.");
        }
    }

    private void validarFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null) {
            throw new BusinessException("La fecha de inicio es obligatoria");
        }
        if (fechaFin != null && fechaFin.isBefore(fechaInicio)) {
            throw new BusinessException("La fecha de fin no puede ser anterior al inicio");
        }
    }

    private void normalizarTipo(Exhibicion exhibicion) {
        exhibicion.setTipo(exhibicion.getFechaFin() == null ? TipoExhibicion.PERMANENTE : TipoExhibicion.TEMPORAL);
    }

    private EstadoExhibicion estadoInicial(LocalDate fechaInicio) {
        return fechaInicio.isAfter(LocalDate.now()) ? EstadoExhibicion.PLANIFICADA : EstadoExhibicion.ACTIVA;
    }

    private boolean bloqueaDisponibilidad(Exhibicion exhibicion) {
        if (exhibicion.getEstado() == EstadoExhibicion.CANCELADA) {
            return false;
        }
        if (exhibicion.getEstado() == EstadoExhibicion.FINALIZADA) {
            return exhibicion.getFechaFin() != null;
        }
        return exhibicion.getEstado() == EstadoExhibicion.PLANIFICADA || exhibicion.getEstado() == EstadoExhibicion.ACTIVA;
    }

    private LocalDate fechaFinEfectiva(Exhibicion exhibicion) {
        if (exhibicion.getEstado() == EstadoExhibicion.FINALIZADA) {
            return exhibicion.getFechaFin();
        }
        return exhibicion.getFechaFin();
    }

    private void liberarObjetosPorFinalizacion(Exhibicion exhibicion, boolean anticipada, String operador) {
        LocalDate hoy = LocalDate.now();
        for (ExhibicionObjeto relacion : exhibicionObjetoRepository.findByExhibicionIdAndEliminadoFalse(exhibicion.getId())) {
            Map<String, Object> anteriores = snapshotExhibicion(relacion);
            relacion.setEstado(EstadoExhibicionObjeto.DEVUELTO);
            relacion.setDevolucionVerificada(true);
            relacion.setFechaRetiro(hoy);
            ExhibicionObjeto saved = exhibicionObjetoRepository.save(relacion);
            if (saved == null) {
                saved = relacion;
            }
            auditoriaObjetoService.registrar(
                    saved.getObjetoMuseo(),
                    TipoOperacionAuditoria.MODIFICACION,
                    anticipada ? "FINALIZACION_ANTICIPADA_EXHIBICION" : "FINALIZACION_EXHIBICION",
                    "La exhibición fue finalizada. El objeto quedó disponible desde la fecha de finalización.",
                    "EXHIBICION",
                    anteriores,
                    snapshotExhibicion(saved),
                    operador
            );
        }
    }

    private void liberarObjetosPorCancelacion(Exhibicion exhibicion, String operador) {
        LocalDate hoy = LocalDate.now();
        for (ExhibicionObjeto relacion : exhibicionObjetoRepository.findByExhibicionIdAndEliminadoFalse(exhibicion.getId())) {
            Map<String, Object> anteriores = snapshotExhibicion(relacion);
            relacion.setEstado(EstadoExhibicionObjeto.DEVUELTO);
            relacion.setDevolucionVerificada(true);
            relacion.setFechaRetiro(hoy);
            ExhibicionObjeto saved = exhibicionObjetoRepository.save(relacion);
            if (saved == null) {
                saved = relacion;
            }
            auditoriaObjetoService.registrar(
                    saved.getObjetoMuseo(),
                    TipoOperacionAuditoria.MODIFICACION,
                    "CANCELACION_EXHIBICION",
                    "La exhibición fue cancelada. El objeto asociado quedó disponible.",
                    "EXHIBICION",
                    anteriores,
                    snapshotExhibicion(saved),
                    operador
            );
        }
    }

    private void registrarInicioAutomatico(Exhibicion exhibicion, String operador) {
        for (ExhibicionObjeto relacion : exhibicionObjetoRepository.findByExhibicionIdAndEliminadoFalse(exhibicion.getId())) {
            auditoriaObjetoService.registrar(
                    relacion.getObjetoMuseo(),
                    TipoOperacionAuditoria.MODIFICACION,
                    "INICIO_AUTOMATICO_EXHIBICION",
                    "Exhibición iniciada automáticamente por llegada de fecha programada.",
                    "EXHIBICION",
                    null,
                    snapshotExhibicion(relacion),
                    operador
            );
        }
    }

    private Set<Long> idsUnicos(Set<Long> objetoIds) {
        Set<Long> ids = objetoIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.size() != objetoIds.size()) {
            throw new BusinessException("No se permiten objetos duplicados o inválidos");
        }
        return ids;
    }

    private Set<Long> idsObjetosActuales(Long exhibicionId) {
        return exhibicionObjetoRepository.findByExhibicionIdAndEliminadoFalse(exhibicionId).stream()
                .map(item -> item.getObjetoMuseo().getId())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Map<String, Object> snapshotExhibicion(ExhibicionObjeto entity) {
        return auditoriaObjetoService.mapOf(
                "exhibicionObjetoId", entity.getId(),
                "exhibicionId", entity.getExhibicion() == null ? null : entity.getExhibicion().getId(),
                "exhibicion", entity.getExhibicion() == null ? null : entity.getExhibicion().getNombre(),
                "estado", entity.getEstado(),
                "fechaInclusion", entity.getFechaInclusion(),
                "fechaRetiro", entity.getFechaRetiro()
        );
    }

    private Exhibicion buscarActivo(Long id) {
        Exhibicion entity = exhibicionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Exhibicion no encontrada"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Exhibicion no encontrada");
        }
        return entity;
    }

    private ObjetoMuseo buscarObjetoActivo(Long id) {
        ObjetoMuseo entity = objetoMuseoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Objeto de museo no encontrado"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Objeto de museo no encontrado");
        }
        return entity;
    }
}
