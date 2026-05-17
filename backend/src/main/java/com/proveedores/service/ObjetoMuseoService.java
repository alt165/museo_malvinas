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
import com.proveedores.entity.CategoriaObjeto;
import com.proveedores.entity.Depositante;
import com.proveedores.entity.EstadoInventario;
import com.proveedores.entity.Inventario;
import com.proveedores.entity.MovimientoInventario;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.ObjetoCategoria;
import com.proveedores.entity.ObjetoDepositante;
import com.proveedores.entity.OrigenCargaObjeto;
import com.proveedores.entity.ReciboIngresoObjeto;
import com.proveedores.entity.TipoMovimientoInventario;
import com.proveedores.entity.Ubicacion;
import com.proveedores.entity.Usuario;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.ObjetoMuseoMapper;
import com.proveedores.repository.CategoriaObjetoRepository;
import com.proveedores.repository.DepositanteRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    private final ObjetoDepositanteRepository objetoDepositanteRepository;
    private final ReciboIngresoObjetoRepository reciboIngresoObjetoRepository;
    private final FotoObjetoMuseoRepository fotoObjetoMuseoRepository;
    private final ReciboEscaneadoObjetoMuseoRepository reciboEscaneadoObjetoMuseoRepository;
    private final InventarioRepository inventarioRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final UbicacionRepository ubicacionRepository;
    private final UsuarioRepository usuarioRepository;

    public ObjetoMuseoService(
            ObjetoMuseoRepository objetoMuseoRepository,
            CategoriaObjetoRepository categoriaObjetoRepository,
            ObjetoCategoriaRepository objetoCategoriaRepository,
            DepositanteRepository depositanteRepository,
            ObjetoDepositanteRepository objetoDepositanteRepository,
            ReciboIngresoObjetoRepository reciboIngresoObjetoRepository,
            FotoObjetoMuseoRepository fotoObjetoMuseoRepository,
            ReciboEscaneadoObjetoMuseoRepository reciboEscaneadoObjetoMuseoRepository,
            InventarioRepository inventarioRepository,
            MovimientoInventarioRepository movimientoInventarioRepository,
            UbicacionRepository ubicacionRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.objetoMuseoRepository = objetoMuseoRepository;
        this.categoriaObjetoRepository = categoriaObjetoRepository;
        this.objetoCategoriaRepository = objetoCategoriaRepository;
        this.depositanteRepository = depositanteRepository;
        this.objetoDepositanteRepository = objetoDepositanteRepository;
        this.reciboIngresoObjetoRepository = reciboIngresoObjetoRepository;
        this.fotoObjetoMuseoRepository = fotoObjetoMuseoRepository;
        this.reciboEscaneadoObjetoMuseoRepository = reciboEscaneadoObjetoMuseoRepository;
        this.inventarioRepository = inventarioRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.ubicacionRepository = ubicacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public ObjetoMuseoResponseDTO crear(ObjetoMuseoRequestDTO dto) {
        validarNumeroInventarioDisponible(dto.numeroInventario(), null);
        ObjetoMuseo entity = ObjetoMuseoMapper.toEntity(dto);
        entity.setOrigenCarga(OrigenCargaObjeto.COMPLETA);
        entity.setDatosCompletos(tieneDatosCompletos(dto));
        ObjetoMuseo saved = objetoMuseoRepository.save(entity);
        sincronizarCategorias(saved, dto.categoriaIds());
        if (dto.ubicacionId() != null) {
            crearInventarioInicial(saved, buscarUbicacionActiva(dto.ubicacionId()), "Alta completa", null);
        }
        log.info("event=objeto_museo.created objetoMuseoId={} numeroInventario={}", saved.getId(), saved.getNumeroInventario());
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
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ObjetoMuseoResponseDTO> listarSinColeccion() {
        return objetoMuseoRepository.findByColeccionObjetoIsNullAndEliminadoFalseOrderByNumeroInventarioAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ObjetoMuseoResponseDTO> buscar(String nombre, String numeroInventario, List<Long> categoriaIds, Pageable pageable) {
        List<Long> categorias = categoriaIds == null
                ? List.of()
                : categoriaIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        return objetoMuseoRepository.findAll(busquedaSpecification(
                        normalizarFiltro(nombre),
                        normalizarFiltro(numeroInventario),
                        categorias
                ), normalizarPageableBusqueda(pageable)).map(this::toResponse);
    }

    @Transactional
    public ObjetoMuseoResponseDTO actualizar(Long id, ObjetoMuseoRequestDTO dto) {
        ObjetoMuseo entity = buscarActivo(id);
        validarNumeroInventarioDisponible(dto.numeroInventario(), id);
        boolean pendienteRapida = entity.getOrigenCarga() == OrigenCargaObjeto.RAPIDA && Boolean.FALSE.equals(entity.getDatosCompletos());
        if (pendienteRapida) {
            validarFichaCompleta(dto);
        }
        ObjetoMuseoMapper.updateEntity(entity, dto);
        if (pendienteRapida || tieneDatosCompletos(dto)) {
            entity.setDatosCompletos(true);
        }
        ObjetoMuseo saved = objetoMuseoRepository.save(entity);
        sincronizarCategorias(saved, dto.categoriaIds());
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
        relacion.setTipoDeposito("RECEPCION");
        relacion.setObservaciones("Carga rapida");
        objetoDepositanteRepository.save(relacion);

        ReciboIngresoObjeto recibo = crearRecibo(saved, depositante, dto.descripcionBreve(), operador);
        ReciboIngresoObjeto reciboSaved = reciboIngresoObjetoRepository.save(recibo);

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
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(LocalDateTime.now());
        entity.setEliminadoPor(eliminadoPor);
        objetoMuseoRepository.save(entity);
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
        if (entity.getEliminado()) {
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

    private Specification<ObjetoMuseo> busquedaSpecification(String nombre, String numeroInventario, List<Long> categoriaIds) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("eliminado")));

            if (nombre != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("denominacionObjeto")),
                        "%" + nombre.toLowerCase(Locale.ROOT) + "%"
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

    private Pageable normalizarPageableBusqueda(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }

        List<Sort.Order> ordenes = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String property = sortPropertyPermitida(order.getProperty());
            if (property != null) {
                ordenes.add(new Sort.Order(order.getDirection(), property, order.getNullHandling()));
            } else {
                log.info("event=objeto_museo.search_sort_ignored property={}", order.getProperty());
            }
        }

        Sort sort = ordenes.isEmpty() ? Sort.unsorted() : Sort.by(ordenes);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
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

    private void validarFichaCompleta(ObjetoMuseoRequestDTO dto) {
        if (!tieneDatosCompletos(dto)) {
            throw new BusinessException("Para completar la ficha se requieren denominacion, descripcion tecnica, materiales, dimensiones, estado de conservacion y al menos una categoria");
        }
    }

    private boolean tieneDatosCompletos(ObjetoMuseoRequestDTO dto) {
        return tieneTexto(dto.denominacionObjeto())
                && tieneTexto(dto.descripcionTecnica())
                && tieneTexto(dto.materiales())
                && tieneTexto(dto.dimensiones())
                && dto.estadoConservacion() != null
                && dto.categoriaIds() != null
                && !dto.categoriaIds().isEmpty();
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

    private ObjetoMuseoResponseDTO toResponse(ObjetoMuseo objeto) {
        List<CategoriaObjetoResponseDTO> categorias = objetoCategoriaRepository.findByObjetoMuseoIdAndEliminadoFalse(objeto.getId()).stream()
                .map(ObjetoCategoria::getCategoriaObjeto)
                .map(categoria -> new CategoriaObjetoResponseDTO(categoria.getId(), categoria.getNombre(), categoria.getDescripcion()))
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
        return ObjetoMuseoMapper.toResponse(objeto, fechaIngreso, ubicacionId, ubicacionNombre, coleccionId, coleccionNombre, categorias, fotos, reciboEscaneado);
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
                objeto.getFechaCargaRapida(),
                objeto.getCargaRapidaPor(),
                reciboId,
                reciboId == null ? null : "/api/recibos/" + reciboId + "/pdf"
        );
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
