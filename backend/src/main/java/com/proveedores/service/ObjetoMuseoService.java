package com.proveedores.service;

import com.proveedores.dto.AgregarCategoriaObjetoRequestDTO;
import com.proveedores.dto.CargaRapidaObjetoRequestDTO;
import com.proveedores.dto.CargaRapidaObjetoResponseDTO;
import com.proveedores.dto.CategoriaObjetoResponseDTO;
import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.dto.ReciboIngresoObjetoResponseDTO;
import com.proveedores.entity.CategoriaObjeto;
import com.proveedores.entity.Depositante;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.ObjetoCategoria;
import com.proveedores.entity.ObjetoDepositante;
import com.proveedores.entity.ReciboIngresoObjeto;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.ObjetoMuseoMapper;
import com.proveedores.repository.CategoriaObjetoRepository;
import com.proveedores.repository.DepositanteRepository;
import com.proveedores.repository.ObjetoCategoriaRepository;
import com.proveedores.repository.ObjetoDepositanteRepository;
import com.proveedores.repository.ObjetoMuseoRepository;
import com.proveedores.repository.ReciboIngresoObjetoRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObjetoMuseoService {

    private static final Logger log = LoggerFactory.getLogger(ObjetoMuseoService.class);

    private final ObjetoMuseoRepository objetoMuseoRepository;
    private final CategoriaObjetoRepository categoriaObjetoRepository;
    private final ObjetoCategoriaRepository objetoCategoriaRepository;
    private final DepositanteRepository depositanteRepository;
    private final ObjetoDepositanteRepository objetoDepositanteRepository;
    private final ReciboIngresoObjetoRepository reciboIngresoObjetoRepository;

    public ObjetoMuseoService(
            ObjetoMuseoRepository objetoMuseoRepository,
            CategoriaObjetoRepository categoriaObjetoRepository,
            ObjetoCategoriaRepository objetoCategoriaRepository,
            DepositanteRepository depositanteRepository,
            ObjetoDepositanteRepository objetoDepositanteRepository,
            ReciboIngresoObjetoRepository reciboIngresoObjetoRepository
    ) {
        this.objetoMuseoRepository = objetoMuseoRepository;
        this.categoriaObjetoRepository = categoriaObjetoRepository;
        this.objetoCategoriaRepository = objetoCategoriaRepository;
        this.depositanteRepository = depositanteRepository;
        this.objetoDepositanteRepository = objetoDepositanteRepository;
        this.reciboIngresoObjetoRepository = reciboIngresoObjetoRepository;
    }

    @Transactional
    public ObjetoMuseoResponseDTO crear(ObjetoMuseoRequestDTO dto) {
        validarNumeroInventarioDisponible(dto.numeroInventario(), null);
        ObjetoMuseo saved = objetoMuseoRepository.save(ObjetoMuseoMapper.toEntity(dto));
        sincronizarCategorias(saved, dto.categoriaIds());
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
    public Page<ObjetoMuseoResponseDTO> buscar(String nombre, String numeroInventario, List<Long> categoriaIds, Pageable pageable) {
        List<Long> categorias = categoriaIds == null
                ? List.of()
                : categoriaIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        return objetoMuseoRepository.findAll(busquedaSpecification(
                        normalizarFiltro(nombre),
                        normalizarFiltro(numeroInventario),
                        categorias
                ), pageable).map(this::toResponse);
    }

    @Transactional
    public ObjetoMuseoResponseDTO actualizar(Long id, ObjetoMuseoRequestDTO dto) {
        ObjetoMuseo entity = buscarActivo(id);
        validarNumeroInventarioDisponible(dto.numeroInventario(), id);
        ObjetoMuseoMapper.updateEntity(entity, dto);
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

        ObjetoMuseo objeto = new ObjetoMuseo();
        objeto.setNumeroInventario(dto.numeroInventario());
        objeto.setDenominacionObjeto(dto.denominacionObjeto());
        objeto.setDescripcion(dto.descripcionBreve());
        ObjetoMuseo saved = objetoMuseoRepository.save(objeto);

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
            query.distinct(true);
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

    private ObjetoMuseoResponseDTO toResponse(ObjetoMuseo objeto) {
        List<CategoriaObjetoResponseDTO> categorias = objetoCategoriaRepository.findByObjetoMuseoIdAndEliminadoFalse(objeto.getId()).stream()
                .map(ObjetoCategoria::getCategoriaObjeto)
                .map(categoria -> new CategoriaObjetoResponseDTO(categoria.getId(), categoria.getNombre(), categoria.getDescripcion()))
                .toList();
        return ObjetoMuseoMapper.toResponse(objeto, categorias);
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
