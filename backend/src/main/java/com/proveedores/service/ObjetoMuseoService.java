package com.proveedores.service;

import com.proveedores.dto.AgregarCategoriaObjetoRequestDTO;
import com.proveedores.dto.CargaRapidaObjetoRequestDTO;
import com.proveedores.dto.CargaRapidaObjetoResponseDTO;
import com.proveedores.dto.CategoriaObjetoResponseDTO;
import com.proveedores.dto.FotoObjetoMuseoResponseDTO;
import com.proveedores.dto.MoverObjetoRequestDTO;
import com.proveedores.dto.MovimientoObjetoResponseDTO;
import com.proveedores.dto.ObjetoMuseoEliminadoResponseDTO;
import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.dto.ObjetoPendienteCompletarResponseDTO;
import com.proveedores.dto.ReciboEscaneadoObjetoMuseoResponseDTO;
import com.proveedores.dto.ReciboIngresoObjetoResponseDTO;
import com.proveedores.entity.CaracterRecepcionObjeto;
import com.proveedores.entity.CategoriaObjeto;
import com.proveedores.entity.Depositante;
import com.proveedores.entity.EmbargoObjeto;
import com.proveedores.entity.EstadoInventario;
import com.proveedores.entity.Inventario;
import com.proveedores.entity.MovimientoInventario;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.ObjetoCategoria;
import com.proveedores.entity.ObjetoDepositante;
import com.proveedores.entity.OrigenCargaObjeto;
import com.proveedores.entity.ReciboIngresoObjeto;
import com.proveedores.entity.TipoMovimientoInventario;
import com.proveedores.entity.TipoOperacionAuditoria;
import com.proveedores.entity.VisibilidadCampo;
import com.proveedores.entity.Ubicacion;
import com.proveedores.entity.Usuario;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.ObjetoMuseoMapper;
import com.proveedores.repository.CategoriaObjetoRepository;
import com.proveedores.repository.DepositanteRepository;
import com.proveedores.repository.EmbargoObjetoRepository;
import com.proveedores.repository.FotoObjetoMuseoRepository;
import com.proveedores.repository.InventarioRepository;
import com.proveedores.repository.MovimientoInventarioRepository;
import com.proveedores.repository.ObjetoCategoriaRepository;
import com.proveedores.repository.ObjetoDepositanteRepository;
import com.proveedores.repository.ObjetoMuseoRepository;
import com.proveedores.repository.ReciboEscaneadoObjetoMuseoRepository;
import com.proveedores.repository.ReciboIngresoObjetoRepository;
import com.proveedores.repository.UbicacionRepository;
import com.proveedores.repository.UsuarioRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObjetoMuseoService {

    private static final Logger log = LoggerFactory.getLogger(ObjetoMuseoService.class);
    private static final String UBICACION_PRE_INGRESO = "Pre ingreso";

    private final ObjetoMuseoRepository objetoMuseoRepository;
    private final CategoriaObjetoRepository categoriaObjetoRepository;
    private final ObjetoCategoriaRepository objetoCategoriaRepository;
    private final DepositanteRepository depositanteRepository;
    private final EmbargoObjetoRepository embargoObjetoRepository;
    private final ObjetoDepositanteRepository objetoDepositanteRepository;
    private final ReciboIngresoObjetoRepository reciboIngresoObjetoRepository;
    private final FotoObjetoMuseoRepository fotoObjetoMuseoRepository;
    private final ReciboEscaneadoObjetoMuseoRepository reciboEscaneadoObjetoMuseoRepository;
    private final InventarioRepository inventarioRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final UbicacionRepository ubicacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaObjetoService auditoriaObjetoService;

    public ObjetoMuseoService(
            ObjetoMuseoRepository objetoMuseoRepository,
            CategoriaObjetoRepository categoriaObjetoRepository,
            ObjetoCategoriaRepository objetoCategoriaRepository,
            DepositanteRepository depositanteRepository,
            EmbargoObjetoRepository embargoObjetoRepository,
            ObjetoDepositanteRepository objetoDepositanteRepository,
            ReciboIngresoObjetoRepository reciboIngresoObjetoRepository,
            FotoObjetoMuseoRepository fotoObjetoMuseoRepository,
            ReciboEscaneadoObjetoMuseoRepository reciboEscaneadoObjetoMuseoRepository,
            InventarioRepository inventarioRepository,
            MovimientoInventarioRepository movimientoInventarioRepository,
            UbicacionRepository ubicacionRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaObjetoService auditoriaObjetoService
    ) {
        this.objetoMuseoRepository = objetoMuseoRepository;
        this.categoriaObjetoRepository = categoriaObjetoRepository;
        this.objetoCategoriaRepository = objetoCategoriaRepository;
        this.depositanteRepository = depositanteRepository;
        this.embargoObjetoRepository = embargoObjetoRepository;
        this.objetoDepositanteRepository = objetoDepositanteRepository;
        this.reciboIngresoObjetoRepository = reciboIngresoObjetoRepository;
        this.fotoObjetoMuseoRepository = fotoObjetoMuseoRepository;
        this.reciboEscaneadoObjetoMuseoRepository = reciboEscaneadoObjetoMuseoRepository;
        this.inventarioRepository = inventarioRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.ubicacionRepository = ubicacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaObjetoService = auditoriaObjetoService;
    }

    @Transactional
    public ObjetoMuseoResponseDTO crear(ObjetoMuseoRequestDTO dto) {
        return crear(dto, null);
    }

    @Transactional
    public ObjetoMuseoResponseDTO crear(ObjetoMuseoRequestDTO dto, String operador) {
        validarNumeroInventarioDisponible(dto.numeroInventario(), null);
        validarRecepcionObligatoria(dto);
        ObjetoMuseo entity = ObjetoMuseoMapper.toEntity(dto);
        entity.setOrigenCarga(OrigenCargaObjeto.COMPLETA);
        entity.setDatosCompletos(tieneDatosCompletos(dto));
        ObjetoMuseo saved = objetoMuseoRepository.save(entity);
        sincronizarCategorias(saved, dto.categoriaIds());
        if (dto.ubicacionId() != null) {
            crearInventarioInicial(saved, buscarUbicacionActiva(dto.ubicacionId()), "Alta completa", operador);
        }
        Depositante depositante = sincronizarRecepcion(saved, dto, "Alta completa");
        ReciboIngresoObjeto reciboSaved = reciboIngresoObjetoRepository.save(crearRecibo(saved, depositante, descripcionRecibo(dto), operador));
        auditoriaObjetoService.registrar(
                saved,
                TipoOperacionAuditoria.CREACION,
                "ALTA_COMPLETA",
                "Alta completa de objeto",
                "ALTA_COMPLETA",
                null,
                snapshotObjeto(saved),
                operador
        );
        log.info("event=objeto_museo.created objetoMuseoId={} numeroInventario={} reciboId={}", saved.getId(), saved.getNumeroInventario(), reciboSaved.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ObjetoMuseoResponseDTO obtenerPorId(Long id) {
        return toResponse(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<ObjetoMuseoResponseDTO> listar() {
        return objetoMuseoRepository.findAll().stream()
                .filter(objeto -> !objeto.getEliminado())
                .filter(this::objetoVisiblePorEmbargo)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ObjetoMuseoResponseDTO> buscarDisponiblesParaColeccion(
            String nombre,
            String numeroInventario,
            List<Long> categoriaIds,
            Long coleccionId,
            Pageable pageable
    ) {
        return objetoMuseoRepository.findAll(
                busquedaDisponiblesParaColeccionSpecification(
                        normalizarFiltro(nombre),
                        normalizarFiltro(numeroInventario),
                        normalizarCategoriaIds(categoriaIds),
                        coleccionId
                ),
                normalizarPageableBusquedaColeccion(pageable)
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ObjetoMuseoResponseDTO> listarSinColeccion() {
        return objetoMuseoRepository.findByColeccionObjetoIsNullAndEliminadoFalseOrderByNumeroInventarioAsc().stream()
                .filter(this::objetoVisiblePorEmbargo)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ObjetoMuseoResponseDTO> buscar(String nombre, String numeroInventario, List<Long> categoriaIds, Pageable pageable) {
        return objetoMuseoRepository.findAll(busquedaSpecification(
                        normalizarFiltro(nombre),
                        normalizarFiltro(numeroInventario),
                        normalizarCategoriaIds(categoriaIds)
                ), normalizarPageableBusqueda(pageable)).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ObjetoMuseoResponseDTO> buscarParaExportacion(String nombre, String numeroInventario, List<Long> categoriaIds, Sort sort) {
        return objetoMuseoRepository.findAll(busquedaSpecification(
                        normalizarFiltro(nombre),
                        normalizarFiltro(numeroInventario),
                        normalizarCategoriaIds(categoriaIds)
                ), normalizarSortBusqueda(sort)).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ObjetoMuseoResponseDTO actualizar(Long id, ObjetoMuseoRequestDTO dto) {
        return actualizar(id, dto, null);
    }

    @Transactional
    public ObjetoMuseoResponseDTO actualizar(Long id, ObjetoMuseoRequestDTO dto, String operador) {
        ObjetoMuseo entity = buscarActivo(id);
        Map<String, Object> anteriores = snapshotObjeto(entity);
        validarNumeroInventarioDisponible(dto.numeroInventario(), id);
        boolean pendienteRapida = entity.getOrigenCarga() == OrigenCargaObjeto.RAPIDA && Boolean.FALSE.equals(entity.getDatosCompletos());
        if (pendienteRapida) {
            validarFichaCompleta(dto);
            validarRecepcionObligatoria(dto);
        }
        ObjetoMuseoMapper.updateEntity(entity, dto);
        if (pendienteRapida || tieneDatosCompletos(dto)) {
            entity.setDatosCompletos(true);
        }
        ObjetoMuseo saved = objetoMuseoRepository.save(entity);
        sincronizarCategorias(saved, dto.categoriaIds());
        if (dto.depositanteId() != null || dto.caracterRecepcion() != null || dto.fechaVencimiento() != null) {
            sincronizarRecepcion(saved, dto, pendienteRapida ? "Completar carga" : "Actualizacion de objeto");
        }
        String accion = pendienteRapida ? "COMPLETAR_CARGA" : "EDICION";
        String descripcion = pendienteRapida
                ? "Completar carga de objeto creado por alta rápida"
                : "Edición de datos generales del objeto";
        auditoriaObjetoService.registrar(
                saved,
                TipoOperacionAuditoria.MODIFICACION,
                accion,
                descripcion,
                accion,
                anteriores,
                snapshotObjeto(saved),
                operador
        );
        log.info("event=objeto_museo.updated objetoMuseoId={} numeroInventario={}", saved.getId(), saved.getNumeroInventario());
        return toResponse(saved);
    }

    @Transactional
    public ObjetoMuseoResponseDTO agregarCategoria(Long id, AgregarCategoriaObjetoRequestDTO dto) {
        ObjetoMuseo objeto = buscarActivo(id);
        CategoriaObjeto categoria = buscarCategoriaActiva(dto.categoriaId());
        if (objetoCategoriaRepository.existsByObjetoMuseoIdAndCategoriaObjetoIdAndEliminadoFalse(id, dto.categoriaId())) {
            throw new BusinessException("El objeto ya tiene asociada esa categoria");
        }

        ObjetoCategoria relacion = new ObjetoCategoria();
        relacion.setObjetoMuseo(objeto);
        relacion.setCategoriaObjeto(categoria);
        relacion.setObservaciones(dto.observaciones());
        objetoCategoriaRepository.save(relacion);
        return toResponse(objeto);
    }

    @Transactional
    public ObjetoMuseoResponseDTO quitarCategoria(Long id, Long categoriaId) {
        ObjetoMuseo objeto = buscarActivo(id);
        ObjetoCategoria relacion = objetoCategoriaRepository.findByObjetoMuseoIdAndEliminadoFalse(id).stream()
                .filter(item -> item.getCategoriaObjeto().getId().equals(categoriaId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Categoria del objeto no encontrada"));
        relacion.setActivo(false);
        relacion.setEliminado(true);
        relacion.setFechaEliminacion(LocalDateTime.now());
        objetoCategoriaRepository.save(relacion);
        return toResponse(objeto);
    }

    @Transactional
    public CargaRapidaObjetoResponseDTO cargaRapida(CargaRapidaObjetoRequestDTO dto, String operador) {
        validarNumeroInventarioDisponible(dto.numeroInventario(), null);
        Depositante depositante = buscarDepositanteActivo(dto.depositanteId());
        LocalDateTime fechaCargaRapida = LocalDateTime.now();

        ObjetoMuseo objeto = new ObjetoMuseo();
        objeto.setNumeroInventario(dto.numeroInventario());
        objeto.setDenominacionObjeto(dto.denominacionObjeto());
        objeto.setDescripcion(dto.descripcionBreve());
        objeto.setOrigenCarga(OrigenCargaObjeto.RAPIDA);
        objeto.setDatosCompletos(false);
        objeto.setFechaCargaRapida(fechaCargaRapida);
        objeto.setCargaRapidaPor(operador);
        ObjetoMuseo saved = objetoMuseoRepository.save(objeto);
        crearInventarioInicial(saved, buscarUbicacionPreIngreso(), "Alta rapida", operador);

        ObjetoDepositante relacion = new ObjetoDepositante();
        relacion.setObjetoMuseo(saved);
        relacion.setDepositante(depositante);
        relacion.setFechaDeposito(LocalDate.now());
        relacion.setTipoDeposito(CaracterRecepcionObjeto.RECEPCION);
        relacion.setObservaciones("Carga rapida");
        objetoDepositanteRepository.save(relacion);

        ReciboIngresoObjeto recibo = crearRecibo(saved, depositante, dto.descripcionBreve(), operador);
        ReciboIngresoObjeto reciboSaved = reciboIngresoObjetoRepository.save(recibo);
        auditoriaObjetoService.registrar(
                saved,
                TipoOperacionAuditoria.CREACION,
                "ALTA_RAPIDA",
                "Alta rápida de objeto",
                "ALTA_RAPIDA",
                null,
                snapshotObjeto(saved),
                operador
        );

        log.info("event=objeto_museo.quick_created objetoMuseoId={} reciboId={}", saved.getId(), reciboSaved.getId());
        return new CargaRapidaObjetoResponseDTO(toResponse(saved), toReciboResponse(reciboSaved), "/api/recibos/" + reciboSaved.getId() + "/pdf");
    }

    @Transactional(readOnly = true)
    public Page<ObjetoPendienteCompletarResponseDTO> listarPendientesCompletar(Pageable pageable) {
        return objetoMuseoRepository.findByOrigenCargaAndDatosCompletosFalseAndEliminadoFalse(
                OrigenCargaObjeto.RAPIDA,
                normalizarPageablePendientes(pageable)
        ).map(this::toPendienteResponse);
    }

    @Transactional(readOnly = true)
    public List<ObjetoMuseoResponseDTO> listarPendientesCompletarParaExportacion(Sort sort) {
        return objetoMuseoRepository.findByOrigenCargaAndDatosCompletosFalseAndEliminadoFalse(
                OrigenCargaObjeto.RAPIDA,
                normalizarSortPendientes(sort)
        ).stream().map(this::toResponseForRelations).toList();
    }

    @Transactional
    public MovimientoObjetoResponseDTO mover(Long id, MoverObjetoRequestDTO dto, String usuarioMovimiento) {
        ObjetoMuseo objeto = buscarActivo(id);
        Ubicacion destino = buscarUbicacionActiva(dto.ubicacionDestinoId());
        java.util.Optional<Inventario> inventarioActual = inventarioRepository.findByObjetoMuseoIdAndEliminadoFalse(id);
        Inventario inventario = inventarioActual.orElseGet(() -> crearInventarioInicialSinMovimiento(objeto, destino));
        Ubicacion origen = inventarioActual.map(Inventario::getUbicacion).orElse(null);
        LocalDateTime fecha = LocalDateTime.now();

        inventario.setUbicacion(destino);
        inventario.setFechaUltimoMovimiento(fecha);
        inventario.setObservaciones(dto.descripcion());
        inventarioRepository.save(inventario);

        MovimientoInventario movimiento = registrarMovimiento(
                objeto,
                inventarioActual.isEmpty() ? TipoMovimientoInventario.INGRESO : TipoMovimientoInventario.CAMBIO_UBICACION,
                origen,
                destino,
                dto.descripcion(),
                usuarioMovimiento,
                fecha
        );
        auditoriaObjetoService.registrar(
                objeto,
                TipoOperacionAuditoria.MODIFICACION,
                "CAMBIO_UBICACION",
                "Cambio de ubicación del objeto",
                "MOVIMIENTO",
                auditoriaObjetoService.mapOf(
                        "ubicacionId", origen == null ? null : origen.getId(),
                        "ubicacion", origen == null ? null : origen.getNombre()
                ),
                auditoriaObjetoService.mapOf(
                        "ubicacionId", destino.getId(),
                        "ubicacion", destino.getNombre(),
                        "descripcion", dto.descripcion()
                ),
                usuarioMovimiento
        );
        log.info("event=objeto_museo.moved objetoMuseoId={} ubicacionOrigenId={} ubicacionDestinoId={}", id, origen == null ? null : origen.getId(), destino.getId());
        return toMovimientoObjetoResponse(movimiento, usuarioMovimiento);
    }

    @Transactional(readOnly = true)
    public List<MovimientoObjetoResponseDTO> listarMovimientos(Long id) {
        buscarActivo(id);
        return movimientoInventarioRepository.findByObjetoMuseoIdAndEliminadoFalseOrderByFechaDesc(id).stream()
                .map(movimiento -> toMovimientoObjetoResponse(movimiento, null))
                .toList();
    }

    @Transactional
    public void bajaLogica(Long id, String eliminadoPor) {
        ObjetoMuseo entity = buscarActivo(id);
        Map<String, Object> anteriores = snapshotObjeto(entity);
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(LocalDateTime.now());
        entity.setEliminadoPor(eliminadoPor);
        objetoMuseoRepository.save(entity);
        auditoriaObjetoService.registrar(
                entity,
                TipoOperacionAuditoria.ELIMINACION,
                "BAJA_LOGICA",
                "Baja lógica de objeto",
                "EDICION",
                anteriores,
                auditoriaObjetoService.mapOf("eliminado", true, "eliminadoPor", eliminadoPor),
                eliminadoPor
        );
        log.info("event=objeto_museo.deleted objetoMuseoId={} numeroInventario={} eliminadoPor={}", entity.getId(), entity.getNumeroInventario(), eliminadoPor);
    }

    @Transactional(readOnly = true)
    public Page<ObjetoMuseoEliminadoResponseDTO> listarEliminados(Pageable pageable) {
        return objetoMuseoRepository.findAll((root, query, criteriaBuilder) ->
                criteriaBuilder.isTrue(root.get("eliminado")), pageable
        ).map(this::toEliminadoResponse);
    }

    @Transactional
    public ObjetoMuseoResponseDTO restaurar(Long id, String restauradoPor) {
        ObjetoMuseo entity = objetoMuseoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Objeto de museo no encontrado"));
        if (!entity.getEliminado()) {
            return toResponse(entity);
        }

        entity.setActivo(true);
        entity.setEliminado(false);
        entity.setFechaEliminacion(null);
        entity.setEliminadoPor(null);
        ObjetoMuseo saved = objetoMuseoRepository.save(entity);

        inventarioRepository.findByObjetoMuseoId(id)
                .filter(inventario -> !inventario.getEliminado())
                .ifPresent(this::restaurarInventarioActivo);

        log.info("event=objeto_museo.restored objetoMuseoId={} numeroInventario={} restauradoPor={}", saved.getId(), saved.getNumeroInventario(), restauradoPor);
        return toResponse(saved);
    }

    private ObjetoMuseo buscarActivo(Long id) {
        ObjetoMuseo entity = objetoMuseoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Objeto de museo no encontrado"));
        if (entity.getEliminado() || !objetoVisiblePorEmbargo(entity)) {
            throw new ResourceNotFoundException("Objeto de museo no encontrado");
        }
        return entity;
    }

    private void restaurarInventarioActivo(Inventario inventario) {
        inventario.setEstado(EstadoInventario.DISPONIBLE);
        inventario.setFechaUltimoMovimiento(LocalDateTime.now());
        inventarioRepository.save(inventario);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setObjetoMuseo(inventario.getObjetoMuseo());
        movimiento.setTipo(TipoMovimientoInventario.RESTAURACION);
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setUbicacionDestino(inventario.getUbicacion());
        movimiento.setObservaciones("Restauracion de objeto eliminado logicamente");
        movimientoInventarioRepository.save(movimiento);
    }

    ObjetoMuseo buscarObjetoActivo(Long id) {
        return buscarActivo(id);
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

    private String normalizarFiltro(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private List<Long> normalizarCategoriaIds(List<Long> categoriaIds) {
        return categoriaIds == null
                ? List.of()
                : categoriaIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
    }

    private Specification<ObjetoMuseo> busquedaSpecification(String nombre, String numeroInventario, List<Long> categoriaIds) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("eliminado")));
            agregarFiltroEmbargoSiCorresponde(root, query, criteriaBuilder, predicates);

            if (nombre != null) {
                String texto = "%" + nombre.toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("denominacionObjeto")), texto),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("descripcion")), texto),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("descripcionTecnica")), texto)
                ));
            }

            if (numeroInventario != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("numeroInventario")),
                        "%" + numeroInventario.toLowerCase(Locale.ROOT) + "%"
                ));
            }

            if (!categoriaIds.isEmpty()) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<ObjetoCategoria> objetoCategoria = subquery.from(ObjetoCategoria.class);
                subquery.select(criteriaBuilder.literal(1L));
                subquery.where(
                        criteriaBuilder.equal(objetoCategoria.get("objetoMuseo"), root),
                        criteriaBuilder.isFalse(objetoCategoria.get("eliminado")),
                        criteriaBuilder.isFalse(objetoCategoria.get("categoriaObjeto").get("eliminado")),
                        objetoCategoria.get("categoriaObjeto").get("id").in(categoriaIds)
                );
                predicates.add(criteriaBuilder.exists(subquery));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void agregarFiltroEmbargoSiCorresponde(Root<ObjetoMuseo> root, jakarta.persistence.criteria.CriteriaQuery<?> query, jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder, List<Predicate> predicates) {
        if (puedeVerObjetosEmbargados()) {
            return;
        }
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<EmbargoObjeto> embargo = subquery.from(EmbargoObjeto.class);
        subquery.select(criteriaBuilder.literal(1L));
        subquery.where(
                criteriaBuilder.equal(embargo.get("objetoMuseo"), root),
                criteriaBuilder.isFalse(embargo.get("eliminado")),
                criteriaBuilder.isNull(embargo.get("fechaFinalizacion"))
        );
        predicates.add(criteriaBuilder.not(criteriaBuilder.exists(subquery)));
    }

    private Specification<ObjetoMuseo> busquedaDisponiblesParaColeccionSpecification(String nombre, String numeroInventario, List<Long> categoriaIds, Long coleccionId) {
        return (root, query, criteriaBuilder) -> {
            Predicate base = busquedaSpecification(nombre, numeroInventario, categoriaIds).toPredicate(root, query, criteriaBuilder);
            Predicate activos = criteriaBuilder.isTrue(root.get("activo"));
            Predicate sinColeccion = criteriaBuilder.isNull(root.get("coleccionObjeto"));
            Predicate disponibles = coleccionId == null
                    ? sinColeccion
                    : criteriaBuilder.or(sinColeccion, criteriaBuilder.equal(root.get("coleccionObjeto").get("id"), coleccionId));
            return criteriaBuilder.and(base, activos, disponibles);
        };
    }

    private Pageable normalizarPageableBusquedaColeccion(Pageable pageable) {
        int size = Math.min(Math.max(pageable.getPageSize(), 1), 50);
        return PageRequest.of(pageable.getPageNumber(), size, normalizarSortBusqueda(pageable.getSort()));
    }

    private Pageable normalizarPageableBusqueda(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), normalizarSortBusqueda(pageable.getSort()));
    }

    private Sort normalizarSortBusqueda(Sort sort) {
        if (sort.isUnsorted()) {
            return Sort.unsorted();
        }

        List<Sort.Order> ordenes = new ArrayList<>();
        for (Sort.Order order : sort) {
            String property = sortPropertyPermitida(order.getProperty());
            if (property != null) {
                ordenes.add(new Sort.Order(order.getDirection(), property, order.getNullHandling()));
            } else {
                log.info("event=objeto_museo.search_sort_ignored property={}", order.getProperty());
            }
        }

        return ordenes.isEmpty() ? Sort.unsorted() : Sort.by(ordenes);
    }

    private String sortPropertyPermitida(String property) {
        return switch (property) {
            case "numeroInventario", "denominacionObjeto", "descripcion", "descripcionTecnica", "estadoConservacion" -> property;
            case "nombre", "denominacion" -> "denominacionObjeto";
            case "fechaIngreso" -> "inventario.fechaIngreso";
            case "categorias", "categoria" -> null;
            default -> null;
        };
    }

    private Pageable normalizarPageablePendientes(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "fechaCargaRapida"));
        }

        List<Sort.Order> ordenes = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String property = switch (order.getProperty()) {
                case "fechaCargaRapida", "numeroInventario", "denominacionObjeto" -> order.getProperty();
                case "nombre", "denominacion" -> "denominacionObjeto";
                default -> null;
            };
            if (property != null) {
                ordenes.add(new Sort.Order(order.getDirection(), property, order.getNullHandling()));
            } else {
                log.info("event=objeto_museo.pending_sort_ignored property={}", order.getProperty());
            }
        }

        Sort sort = ordenes.isEmpty() ? Sort.by(Sort.Direction.ASC, "fechaCargaRapida") : Sort.by(ordenes);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private Sort normalizarSortPendientes(Sort sort) {
        Pageable pageable = PageRequest.of(0, 1, sort == null ? Sort.unsorted() : sort);
        return normalizarPageablePendientes(pageable).getSort();
    }

    private void validarFichaCompleta(ObjetoMuseoRequestDTO dto) {
        if (!tieneDatosCompletos(dto)) {
            throw new BusinessException("Para completar la ficha se requieren denominacion, descripcion tecnica, materiales, al menos una dimension, estado de conservacion y al menos una categoria");
        }
    }

    private void validarRecepcionObligatoria(ObjetoMuseoRequestDTO dto) {
        if (dto.depositanteId() == null) {
            throw new BusinessException("El depositante es obligatorio");
        }
        if (dto.caracterRecepcion() == null || dto.caracterRecepcion() == CaracterRecepcionObjeto.RECEPCION) {
            throw new BusinessException("El caracter de recepcion es obligatorio");
        }
        validarFechaVencimiento(dto, null);
    }

    private void validarFechaVencimiento(ObjetoMuseoRequestDTO dto, LocalDate fechaIngreso) {
        boolean requiereVencimiento = requiereFechaVencimiento(dto.caracterRecepcion());
        if (requiereVencimiento && dto.fechaVencimiento() == null) {
            throw new BusinessException("La fecha de vencimiento es obligatoria para prestamo o comodato");
        }
        if (dto.fechaVencimiento() != null) {
            LocalDate fechaBase = fechaIngreso == null ? LocalDate.now() : fechaIngreso;
            if (dto.fechaVencimiento().isBefore(fechaBase)) {
                throw new BusinessException("La fecha de vencimiento no puede ser anterior a la fecha de ingreso");
            }
        }
    }

    private boolean requiereFechaVencimiento(CaracterRecepcionObjeto caracter) {
        return caracter == CaracterRecepcionObjeto.PRESTAMO || caracter == CaracterRecepcionObjeto.COMODATO;
    }

    private Depositante sincronizarRecepcion(ObjetoMuseo objeto, ObjetoMuseoRequestDTO dto, String observaciones) {
        validarRecepcionObligatoria(dto);
        Inventario inventario = inventarioRepository.findByObjetoMuseoIdAndEliminadoFalse(objeto.getId()).orElse(null);
        LocalDate fechaIngreso = inventario == null ? LocalDate.now() : inventario.getFechaIngreso();
        validarFechaVencimiento(dto, fechaIngreso);
        Depositante depositante = buscarDepositanteActivo(dto.depositanteId());

        ObjetoDepositante relacion = objetoDepositanteRepository.findFirstByObjetoMuseoIdAndEliminadoFalseOrderByIdAsc(objeto.getId())
                .orElseGet(ObjetoDepositante::new);
        relacion.setObjetoMuseo(objeto);
        relacion.setDepositante(depositante);
        relacion.setFechaDeposito(fechaIngreso);
        relacion.setTipoDeposito(dto.caracterRecepcion());
        relacion.setFechaVencimiento(requiereFechaVencimiento(dto.caracterRecepcion()) ? dto.fechaVencimiento() : null);
        relacion.setObservaciones(observaciones);
        objetoDepositanteRepository.save(relacion);
        return depositante;
    }


    private Map<String, Object> snapshotObjeto(ObjetoMuseo objeto) {
        Inventario inventario = inventarioRepository.findByObjetoMuseoIdAndEliminadoFalse(objeto.getId()).orElse(null);
        ObjetoDepositante relacion = objetoDepositanteRepository
                .findFirstByObjetoMuseoIdAndEliminadoFalseOrderByIdAsc(objeto.getId())
                .orElse(null);
        return auditoriaObjetoService.mapOf(
                "numeroInventario", objeto.getNumeroInventario(),
                "denominacionObjeto", objeto.getDenominacionObjeto(),
                "descripcion", objeto.getDescripcion(),
                "descripcionTecnica", objeto.getDescripcionTecnica(),
                "materiales", objeto.getMateriales(),
                "alto", objeto.getAlto(),
                "ancho", objeto.getAncho(),
                "diametro", objeto.getDiametro(),
                "espesor", objeto.getEspesor(),
                "peso", objeto.getPeso(),
                "inscripciones", objeto.getInscripciones(),
                "regimenPropiedad", objeto.getRegimenPropiedad(),
                "condicionLegalBien", objeto.getCondicionLegalBien(),
                "estadoConservacion", objeto.getEstadoConservacion(),
                "detallesEstadoConservacion", objeto.getDetallesEstadoConservacion(),
                "intervencionesInadecuadas", objeto.getIntervencionesInadecuadas(),
                "estadoIntegridad", objeto.getEstadoIntegridad(),
                "humedadConservacion", objeto.getHumedadConservacion(),
                "temperaturaConservacion", objeto.getTemperaturaConservacion(),
                "luzConservacion", objeto.getLuzConservacion(),
                "conservacionExtintores", objeto.getConservacionExtintores(),
                "conservacionMontaje", objeto.getConservacionMontaje(),
                "conservacionSistemaElectrico", objeto.getConservacionSistemaElectrico(),
                "conservacionAlarmas", objeto.getConservacionAlarmas(),
                "conservacionCamaras", objeto.getConservacionCamaras(),
                "visibilidades", objeto.getVisibilidades(),
                "origenCarga", objeto.getOrigenCarga(),
                "datosCompletos", objeto.getDatosCompletos(),
                "ubicacionId", inventario == null || inventario.getUbicacion() == null ? null : inventario.getUbicacion().getId(),
                "ubicacion", inventario == null || inventario.getUbicacion() == null ? null : inventario.getUbicacion().getNombre(),
                "fechaIngreso", inventario == null ? null : inventario.getFechaIngreso(),
                "depositanteId", relacion == null || relacion.getDepositante() == null ? null : relacion.getDepositante().getId(),
                "depositante", relacion == null || relacion.getDepositante() == null ? null : relacion.getDepositante().getNombre(),
                "caracterRecepcion", relacion == null ? null : relacion.getTipoDeposito(),
                "fechaVencimiento", relacion == null ? null : relacion.getFechaVencimiento()
        );
    }

    private boolean tieneDatosCompletos(ObjetoMuseoRequestDTO dto) {
        return tieneTexto(dto.denominacionObjeto())
                && tieneTexto(dto.descripcionTecnica())
                && tieneTexto(dto.materiales())
                && tieneAlgunaDimension(dto)
                && dto.estadoConservacion() != null
                && dto.categoriaIds() != null
                && !dto.categoriaIds().isEmpty();
    }

    private boolean tieneAlgunaDimension(ObjetoMuseoRequestDTO dto) {
        return tieneTexto(dto.alto())
                || tieneTexto(dto.ancho())
                || tieneTexto(dto.diametro())
                || tieneTexto(dto.espesor())
                || tieneTexto(dto.peso());
    }

    private boolean tieneTexto(String value) {
        return value != null && !value.isBlank();
    }

    private void sincronizarCategorias(ObjetoMuseo objeto, Set<Long> categoriaIds) {
        if (categoriaIds == null) {
            return;
        }
        Set<Long> idsUnicos = new HashSet<>(categoriaIds);
        if (idsUnicos.size() != categoriaIds.size()) {
            throw new BusinessException("No se permiten categorias duplicadas");
        }

        List<ObjetoCategoria> existentes = objetoCategoriaRepository.findByObjetoMuseoIdAndEliminadoFalse(objeto.getId());
        for (ObjetoCategoria existente : existentes) {
            if (!idsUnicos.contains(existente.getCategoriaObjeto().getId())) {
                existente.setActivo(false);
                existente.setEliminado(true);
                existente.setFechaEliminacion(LocalDateTime.now());
                objetoCategoriaRepository.save(existente);
            }
        }

        Set<Long> actuales = existentes.stream()
                .map(item -> item.getCategoriaObjeto().getId())
                .collect(java.util.stream.Collectors.toSet());
        for (Long categoriaId : idsUnicos) {
            if (!actuales.contains(categoriaId)) {
                CategoriaObjeto categoria = buscarCategoriaActiva(categoriaId);
                ObjetoCategoria relacion = new ObjetoCategoria();
                relacion.setObjetoMuseo(objeto);
                relacion.setCategoriaObjeto(categoria);
                objetoCategoriaRepository.save(relacion);
            }
        }
    }

    private CategoriaObjeto buscarCategoriaActiva(Long id) {
        CategoriaObjeto categoria = categoriaObjetoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));
        if (categoria.getEliminado()) {
            throw new ResourceNotFoundException("Categoria no encontrada");
        }
        return categoria;
    }

    private Depositante buscarDepositanteActivo(Long id) {
        Depositante depositante = depositanteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Depositante no encontrado"));
        if (depositante.getEliminado()) {
            throw new ResourceNotFoundException("Depositante no encontrado");
        }
        return depositante;
    }

    ObjetoMuseoResponseDTO toResponseForRelations(ObjetoMuseo objeto) {
        return toResponse(objeto);
    }

    private boolean objetoVisiblePorEmbargo(ObjetoMuseo objeto) {
        return puedeVerObjetosEmbargados()
                || !embargoObjetoRepository.existsByObjetoMuseoIdAndFechaFinalizacionIsNullAndEliminadoFalse(objeto.getId());
    }

    private boolean puedeVerObjetosEmbargados() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority) || "ROLE_OPERATOR".equals(authority));
    }

    private ObjetoMuseoResponseDTO toResponse(ObjetoMuseo objeto) {
        List<CategoriaObjetoResponseDTO> categorias = objetoCategoriaRepository.findByObjetoMuseoIdAndEliminadoFalse(objeto.getId()).stream()
                .map(ObjetoCategoria::getCategoriaObjeto)
                .map(categoria -> new CategoriaObjetoResponseDTO(categoria.getId(), categoria.getNombre(), categoria.getDescripcion()))
                .sorted(Comparator.comparing(CategoriaObjetoResponseDTO::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
        Inventario inventario = inventarioRepository.findByObjetoMuseoIdAndEliminadoFalse(objeto.getId()).orElse(null);
        LocalDate fechaIngreso = inventario == null ? null : inventario.getFechaIngreso();
        Long ubicacionId = inventario == null || inventario.getUbicacion() == null ? null : inventario.getUbicacion().getId();
        String ubicacionNombre = inventario == null || inventario.getUbicacion() == null ? null : inventario.getUbicacion().getNombre();
        Long coleccionId = objeto.getColeccionObjeto() == null ? null : objeto.getColeccionObjeto().getId();
        String coleccionNombre = objeto.getColeccionObjeto() == null ? null : objeto.getColeccionObjeto().getNombre();
        List<FotoObjetoMuseoResponseDTO> fotos = fotoObjetoMuseoRepository.findByObjetoMuseoIdAndEliminadoFalse(objeto.getId()).stream()
                .map(foto -> new FotoObjetoMuseoResponseDTO(
                        foto.getId(),
                        foto.getObjetoMuseo().getId(),
                        foto.getNombreArchivo(),
                        foto.getNombreArchivoAlmacenado(),
                        foto.getContentType(),
                        foto.getTamanioBytes(),
                        foto.getDescripcion(),
                        foto.getVisibilidad(),
                        foto.getFechaCarga(),
                        foto.getCargadoPor()
                ))
                .toList();
        ReciboEscaneadoObjetoMuseoResponseDTO reciboEscaneado = reciboEscaneadoObjetoMuseoRepository
                .findFirstByObjetoMuseoIdAndEliminadoFalseOrderByFechaCargaDesc(objeto.getId())
                .map(recibo -> new ReciboEscaneadoObjetoMuseoResponseDTO(
                        recibo.getId(),
                        recibo.getObjetoMuseo().getId(),
                        recibo.getNombreArchivoOriginal(),
                        recibo.getContentType(),
                        recibo.getTamanioBytes(),
                        recibo.getFechaCarga(),
                        recibo.getCargadoPor()
                ))
                .orElse(null);
        ObjetoDepositante objetoDepositante = objetoDepositanteRepository.findFirstByObjetoMuseoIdAndEliminadoFalseOrderByIdAsc(objeto.getId())
                .orElse(null);
        Long depositanteId = objetoDepositante == null ? null : objetoDepositante.getDepositante().getId();
        String depositanteNombre = objetoDepositante == null ? null : objetoDepositante.getDepositante().getNombre();
        CaracterRecepcionObjeto caracterRecepcion = objetoDepositante == null ? null : objetoDepositante.getTipoDeposito();
        LocalDate fechaVencimiento = objetoDepositante == null ? null : objetoDepositante.getFechaVencimiento();
        ObjetoMuseoResponseDTO response = ObjetoMuseoMapper.toResponse(objeto, fechaIngreso, ubicacionId, ubicacionNombre, coleccionId, coleccionNombre, depositanteId, depositanteNombre, caracterRecepcion, fechaVencimiento, categorias, fotos, reciboEscaneado);
        return filtrarCamposPrivados(response);
    }

    private ObjetoMuseoResponseDTO filtrarCamposPrivados(ObjetoMuseoResponseDTO response) {
        if (puedeVerCamposPrivados()) {
            return response;
        }
        Map<String, VisibilidadCampo> visibilidades = response.visibilidades() == null ? Map.of() : response.visibilidades();
        boolean tieneCamposPrivados = visibilidades.values().stream().anyMatch(VisibilidadCampo.PRIVADO::equals);
        boolean tieneFotosPrivadas = response.fotos().stream().anyMatch(foto -> foto.visibilidad() == VisibilidadCampo.PRIVADO);
        boolean tieneReciboEscaneado = response.reciboEscaneado() != null;
        if (!tieneCamposPrivados && !tieneFotosPrivadas && !tieneReciboEscaneado) {
            return response;
        }
        return new ObjetoMuseoResponseDTO(
                response.id(),
                visible(response, "numeroInventario") ? response.numeroInventario() : null,
                visible(response, "denominacionObjeto") ? response.denominacionObjeto() : null,
                visible(response, "descripcion") ? response.descripcion() : null,
                visible(response, "descripcionTecnica") ? response.descripcionTecnica() : null,
                visible(response, "materiales") ? response.materiales() : null,
                visible(response, "alto") ? response.alto() : null,
                visible(response, "ancho") ? response.ancho() : null,
                visible(response, "diametro") ? response.diametro() : null,
                visible(response, "espesor") ? response.espesor() : null,
                visible(response, "peso") ? response.peso() : null,
                visible(response, "inscripciones") ? response.inscripciones() : null,
                visible(response, "regimenPropiedad") ? response.regimenPropiedad() : null,
                visible(response, "condicionLegalBien") ? response.condicionLegalBien() : null,
                visible(response, "estadoConservacion") ? response.estadoConservacion() : null,
                visible(response, "detallesEstadoConservacion") ? response.detallesEstadoConservacion() : Set.of(),
                visible(response, "intervencionesInadecuadas") ? response.intervencionesInadecuadas() : null,
                visible(response, "estadoIntegridad") ? response.estadoIntegridad() : null,
                visible(response, "humedadConservacion") ? response.humedadConservacion() : null,
                visible(response, "temperaturaConservacion") ? response.temperaturaConservacion() : null,
                visible(response, "luzConservacion") ? response.luzConservacion() : null,
                visible(response, "conservacionExtintores") ? response.conservacionExtintores() : null,
                visible(response, "conservacionMontaje") ? response.conservacionMontaje() : null,
                visible(response, "conservacionSistemaElectrico") ? response.conservacionSistemaElectrico() : null,
                visible(response, "conservacionAlarmas") ? response.conservacionAlarmas() : null,
                visible(response, "conservacionCamaras") ? response.conservacionCamaras() : null,
                Map.of(),
                visible(response, "fechaIngreso") ? response.fechaIngreso() : null,
                response.origenCarga(),
                response.datosCompletos(),
                response.fechaCargaRapida(),
                response.cargaRapidaPor(),
                visible(response, "ubicacion") ? response.ubicacionId() : null,
                visible(response, "ubicacion") ? response.ubicacionNombre() : null,
                visible(response, "coleccion") ? response.coleccionId() : null,
                visible(response, "coleccion") ? response.coleccionNombre() : null,
                visible(response, "depositante") ? response.depositanteId() : null,
                visible(response, "depositante") ? response.depositanteNombre() : null,
                visible(response, "caracterRecepcion") ? response.caracterRecepcion() : null,
                visible(response, "fechaVencimiento") ? response.fechaVencimiento() : null,
                visible(response, "categorias") ? response.categorias() : List.of(),
                visible(response, "fotos") ? filtrarFotosPrivadas(response.fotos()) : List.of(),
                null
        );
    }

    private List<FotoObjetoMuseoResponseDTO> filtrarFotosPrivadas(List<FotoObjetoMuseoResponseDTO> fotos) {
        if (puedeVerCamposPrivados()) {
            return fotos;
        }
        return fotos.stream()
                .filter(foto -> foto.visibilidad() != VisibilidadCampo.PRIVADO)
                .toList();
    }

    private boolean visible(ObjetoMuseoResponseDTO response, String campo) {
        Map<String, VisibilidadCampo> visibilidades = response.visibilidades() == null ? Map.of() : response.visibilidades();
        return visibilidades.getOrDefault(campo, VisibilidadCampo.PUBLICO) != VisibilidadCampo.PRIVADO;
    }

    private boolean puedeVerCamposPrivados() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority) || "ROLE_OPERATOR".equals(authority));
    }

    private void crearInventarioInicial(ObjetoMuseo objeto, Ubicacion ubicacion, String observaciones, String usuarioMovimiento) {
        Inventario inventario = inventarioRepository.findByObjetoMuseoIdAndEliminadoFalse(objeto.getId())
                .orElseGet(() -> crearInventarioInicialSinMovimiento(objeto, ubicacion));
        Ubicacion origen = inventario.getUbicacion();
        inventario.setUbicacion(ubicacion);
        inventario.setFechaUltimoMovimiento(LocalDateTime.now());
        inventario.setObservaciones(observaciones);
        inventarioRepository.save(inventario);
        registrarMovimiento(objeto, TipoMovimientoInventario.INGRESO, origen == ubicacion ? null : origen, ubicacion, observaciones, usuarioMovimiento, LocalDateTime.now());
    }

    private Inventario crearInventarioInicialSinMovimiento(ObjetoMuseo objeto, Ubicacion ubicacion) {
        Inventario inventario = new Inventario();
        inventario.setObjetoMuseo(objeto);
        inventario.setUbicacion(ubicacion);
        inventario.setEstado(EstadoInventario.DISPONIBLE);
        inventario.setEstadoConservacion(objeto.getEstadoConservacion() == null ? com.proveedores.entity.EstadoConservacion.BUENO : objeto.getEstadoConservacion());
        inventario.setFechaIngreso(LocalDate.now());
        inventario.setFechaUltimoMovimiento(LocalDateTime.now());
        return inventarioRepository.save(inventario);
    }

    private MovimientoInventario registrarMovimiento(
            ObjetoMuseo objeto,
            TipoMovimientoInventario tipo,
            Ubicacion origen,
            Ubicacion destino,
            String observaciones,
            String usuarioMovimiento,
            LocalDateTime fecha
    ) {
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setObjetoMuseo(objeto);
        movimiento.setTipo(tipo);
        movimiento.setFecha(fecha);
        movimiento.setUbicacionOrigen(origen);
        movimiento.setUbicacionDestino(destino);
        resolverUsuario(usuarioMovimiento).ifPresent(movimiento::setUsuario);
        movimiento.setObservaciones(observaciones);
        return movimientoInventarioRepository.save(movimiento);
    }

    private java.util.Optional<Usuario> resolverUsuario(String usuarioMovimiento) {
        if (usuarioMovimiento == null || usuarioMovimiento.isBlank()) {
            return java.util.Optional.empty();
        }
        return usuarioRepository.findByEmailAndEliminadoFalse(usuarioMovimiento);
    }

    private Ubicacion buscarUbicacionPreIngreso() {
        return ubicacionRepository.findByNombreAndEliminadoFalse(UBICACION_PRE_INGRESO)
                .orElseThrow(() -> new ResourceNotFoundException("Ubicacion Pre ingreso no encontrada"));
    }

    private Ubicacion buscarUbicacionActiva(Long id) {
        Ubicacion ubicacion = ubicacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ubicacion no encontrada"));
        if (ubicacion.getEliminado() || !ubicacion.getActivo()) {
            throw new ResourceNotFoundException("Ubicacion no encontrada");
        }
        return ubicacion;
    }

    private MovimientoObjetoResponseDTO toMovimientoObjetoResponse(MovimientoInventario movimiento, String usuarioFallback) {
        return new MovimientoObjetoResponseDTO(
                movimiento.getId(),
                movimiento.getFecha(),
                movimiento.getUbicacionOrigen() == null ? null : movimiento.getUbicacionOrigen().getId(),
                movimiento.getUbicacionOrigen() == null ? null : movimiento.getUbicacionOrigen().getNombre(),
                movimiento.getUbicacionDestino() == null ? null : movimiento.getUbicacionDestino().getId(),
                movimiento.getUbicacionDestino() == null ? null : movimiento.getUbicacionDestino().getNombre(),
                movimiento.getObservaciones(),
                movimiento.getUsuario() == null ? usuarioFallback : movimiento.getUsuario().getNombre(),
                movimiento.getTipo(),
                null,
                null
        );
    }

    private ObjetoMuseoEliminadoResponseDTO toEliminadoResponse(ObjetoMuseo objeto) {
        List<CategoriaObjetoResponseDTO> categorias = objetoCategoriaRepository.findByObjetoMuseoIdAndEliminadoFalse(objeto.getId()).stream()
                .map(ObjetoCategoria::getCategoriaObjeto)
                .map(categoria -> new CategoriaObjetoResponseDTO(categoria.getId(), categoria.getNombre(), categoria.getDescripcion()))
                .sorted(Comparator.comparing(CategoriaObjetoResponseDTO::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new ObjetoMuseoEliminadoResponseDTO(
                objeto.getId(),
                objeto.getNumeroInventario(),
                objeto.getDenominacionObjeto(),
                objeto.getDescripcion(),
                objeto.getFechaEliminacion(),
                objeto.getEliminadoPor(),
                objeto.getEstadoConservacion(),
                categorias
        );
    }

    private ObjetoPendienteCompletarResponseDTO toPendienteResponse(ObjetoMuseo objeto) {
        ObjetoDepositante objetoDepositante = objetoDepositanteRepository.findFirstByObjetoMuseoIdAndEliminadoFalseOrderByIdAsc(objeto.getId())
                .orElse(null);
        ReciboIngresoObjeto recibo = reciboIngresoObjetoRepository.findFirstByObjetoMuseoIdAndEliminadoFalseOrderByFechaEmisionAsc(objeto.getId())
                .orElse(null);
        Long reciboId = recibo == null ? null : recibo.getId();

        return new ObjetoPendienteCompletarResponseDTO(
                objeto.getId(),
                objeto.getNumeroInventario(),
                objeto.getDenominacionObjeto(),
                objeto.getDescripcion(),
                objetoDepositante == null ? null : objetoDepositante.getDepositante().getId(),
                objetoDepositante == null ? null : objetoDepositante.getDepositante().getNombre(),
                objetoDepositante == null ? null : objetoDepositante.getTipoDeposito(),
                objetoDepositante == null ? null : objetoDepositante.getFechaVencimiento(),
                objeto.getFechaCargaRapida(),
                objeto.getCargaRapidaPor(),
                reciboId,
                reciboId == null ? null : "/api/recibos/" + reciboId + "/pdf"
        );
    }

    private String descripcionRecibo(ObjetoMuseoRequestDTO dto) {
        if (tieneTexto(dto.descripcion())) {
            return dto.descripcion();
        }
        return dto.denominacionObjeto();
    }

    private ReciboIngresoObjeto crearRecibo(ObjetoMuseo objeto, Depositante depositante, String descripcionBreve, String operador) {
        LocalDateTime fecha = LocalDateTime.now();
        ReciboIngresoObjeto recibo = new ReciboIngresoObjeto();
        recibo.setNumeroRecibo("REC-" + fecha.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + objeto.getId());
        recibo.setFechaEmision(fecha);
        recibo.setObjetoMuseo(objeto);
        recibo.setDepositante(depositante);
        recibo.setNumeroInventario(objeto.getNumeroInventario());
        recibo.setDenominacionObjeto(objeto.getDenominacionObjeto());
        recibo.setDescripcionBreve(descripcionBreve);
        recibo.setDepositanteNombre(depositante.getNombre());
        recibo.setDepositanteContacto(depositante.getContacto());
        recibo.setOperador(operador);
        recibo.setTextoConstancia("Se deja constancia de la recepcion provisoria del objeto indicado para su registro patrimonial en el museo.");
        return recibo;
    }

    private ReciboIngresoObjetoResponseDTO toReciboResponse(ReciboIngresoObjeto recibo) {
        return ReciboIngresoObjetoService.toResponse(recibo);
    }
}
