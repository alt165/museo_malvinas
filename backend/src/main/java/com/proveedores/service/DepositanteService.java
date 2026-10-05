package com.proveedores.service;

import com.proveedores.dto.DepositanteRequestDTO;
import com.proveedores.dto.DepositanteResponseDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.entity.Depositante;
import com.proveedores.entity.TipoDepositante;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ConflictException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.DepositanteMapper;
import com.proveedores.repository.DepositanteRepository;
import com.proveedores.repository.ObjetoDepositanteRepository;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepositanteService {

    private final DepositanteRepository depositanteRepository;
    private final ObjetoDepositanteRepository objetoDepositanteRepository;
    private final ObjetoMuseoService objetoMuseoService;

    public DepositanteService(
            DepositanteRepository depositanteRepository,
            ObjetoDepositanteRepository objetoDepositanteRepository,
            ObjetoMuseoService objetoMuseoService
    ) {
        this.depositanteRepository = depositanteRepository;
        this.objetoDepositanteRepository = objetoDepositanteRepository;
        this.objetoMuseoService = objetoMuseoService;
    }

    @Transactional
    public DepositanteResponseDTO crear(DepositanteRequestDTO dto) {
        String identificacion = normalizarIdentificacionParaAlta(dto);
        if (identificacion != null) {
            var existente = dto.tipo() == TipoDepositante.PERSONA
                    ? depositanteRepository.findPrimeroPorDniNormalizado(identificacion)
                    : depositanteRepository.findPrimeroPorCuitNormalizado(identificacion);
            if (existente.isPresent()) {
                Depositante depositante = existente.get();
                String tipoIdentificacion = dto.tipo() == TipoDepositante.PERSONA ? "DNI" : "CUIT";
                if (Boolean.TRUE.equals(depositante.getEliminado())) {
                    throw new ConflictException(
                            "Existe un depositante dado de baja con este " + tipoIdentificacion,
                            "DEPOSITANTE_ELIMINADO",
                            depositante.getId(),
                            tipoIdentificacion
                    );
                }
                throw new ConflictException("Ya existe un depositante activo con este " + tipoIdentificacion);
            }
        }
        return DepositanteMapper.toResponse(depositanteRepository.save(DepositanteMapper.toEntity(dto)));
    }

    @Transactional
    public DepositanteResponseDTO restaurar(Long id) {
        Depositante entity = depositanteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Depositante no encontrado"));
        entity.setActivo(true);
        entity.setEliminado(false);
        entity.setFechaEliminacion(null);
        return DepositanteMapper.toResponse(depositanteRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public DepositanteResponseDTO obtenerPorId(Long id) {
        return DepositanteMapper.toResponse(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<DepositanteResponseDTO> listar() {
        return depositanteRepository.findAll().stream().filter(e -> !e.getEliminado()).map(DepositanteMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<DepositanteResponseDTO> buscar(String texto, Pageable pageable) {
        String filtro = texto == null || texto.isBlank() ? null : normalizarTextoBusqueda(texto.trim());
        int size = Math.min(Math.max(pageable.getPageSize(), 1), 50);
        Pageable pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                size,
                Sort.by(Sort.Direction.ASC, "nombre").and(Sort.by(Sort.Direction.ASC, "id"))
        );
        Specification<Depositante> specification = (root, query, criteriaBuilder) -> {
            var noEliminado = criteriaBuilder.isFalse(root.get("eliminado"));
            if (filtro == null) {
                return noEliminado;
            }

            String patronNombre = "%" + escaparPatronLike(filtro) + "%";
            var patronNombreNormalizado = criteriaBuilder.function(
                    "unaccent",
                    String.class,
                    criteriaBuilder.lower(criteriaBuilder.literal(patronNombre))
            );
            var coincideNombre = criteriaBuilder.like(
                    criteriaBuilder.function("unaccent", String.class, criteriaBuilder.lower(root.get("nombre"))),
                    patronNombreNormalizado,
                    '\\'
            );

            String identificacion = filtro.replaceAll("[.\\-\\s]", "");
            var coincideIdentificacion = criteriaBuilder.disjunction();
            if (!identificacion.isEmpty()) {
                String patronIdentificacion = "%" + escaparPatronLike(identificacion) + "%";
                var patronIdentificacionNormalizado = criteriaBuilder.literal(patronIdentificacion);
                var dniNormalizado = normalizarIdentificacionSql(root.get("dni"), criteriaBuilder);
                var cuitNormalizado = normalizarIdentificacionSql(root.get("cuit"), criteriaBuilder);
                coincideIdentificacion = criteriaBuilder.or(
                        criteriaBuilder.like(dniNormalizado, patronIdentificacionNormalizado, '\\'),
                        criteriaBuilder.like(cuitNormalizado, patronIdentificacionNormalizado, '\\')
                );
            }

            return criteriaBuilder.and(noEliminado, criteriaBuilder.or(coincideNombre, coincideIdentificacion));
        };

        return depositanteRepository.findAll(specification, pageRequest).map(DepositanteMapper::toResponse);
    }

    @Transactional
    public DepositanteResponseDTO actualizar(Long id, DepositanteRequestDTO dto) {
        Depositante entity = buscarActivo(id);
        entity.setNombre(dto.nombre());
        entity.setTipo(dto.tipo());
        entity.setContacto(dto.contacto());
        entity.setDni(dto.dni());
        entity.setCuit(dto.cuit());
        entity.setObservaciones(dto.observaciones());
        return DepositanteMapper.toResponse(depositanteRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public DepositanteResponseDTO buscarPorIdentificacion(String valor) {
        String identificacion = normalizarIdentificacion(valor);
        return depositanteRepository.findActivoByIdentificacionNormalizada(identificacion)
                .map(DepositanteMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Depositante no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<DepositanteResponseDTO> buscarPorNombre(String valor) {
        String nombre = normalizarBusquedaNombre(valor);
        String nombreNormalizado = normalizarTexto(nombre);
        LinkedHashMap<Long, Depositante> resultados = new LinkedHashMap<>();

        depositanteRepository.findByNombreContainingIgnoreCaseAndEliminadoFalse(nombre)
                .forEach(depositante -> resultados.put(depositante.getId(), depositante));

        depositanteRepository.findAll().stream()
                .filter(depositante -> !depositante.getEliminado())
                .filter(depositante -> normalizarTexto(depositante.getNombre()).contains(nombreNormalizado))
                .forEach(depositante -> resultados.putIfAbsent(depositante.getId(), depositante));

        return resultados.values().stream().map(DepositanteMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ObjetoMuseoResponseDTO> listarObjetos(Long id) {
        buscarActivo(id);
        return objetoDepositanteRepository.findObjetosActivosPorDepositante(id).stream()
                .map(relacion -> objetoMuseoService.toResponseForRelations(relacion.getObjetoMuseo()))
                .toList();
    }

    @Transactional
    public void bajaLogica(Long id) {
        Depositante entity = buscarActivo(id);
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(com.proveedores.time.MuseoTime.now());
        depositanteRepository.save(entity);
    }

    Depositante buscarActivo(Long id) {
        Depositante entity = depositanteRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Depositante no encontrado"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Depositante no encontrado");
        }
        return entity;
    }

    private String normalizarIdentificacion(String valor) {
        if (valor == null) {
            throw new BusinessException("La identificacion es obligatoria");
        }
        String normalizado = valor.replace(".", "").replace("-", "").replace(" ", "").trim();
        if (normalizado.isBlank()) {
            throw new BusinessException("La identificacion es obligatoria");
        }
        return normalizado;
    }

    private String normalizarIdentificacionParaAlta(DepositanteRequestDTO dto) {
        String valor = dto.tipo() == TipoDepositante.PERSONA ? dto.dni() : dto.cuit();
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String normalizado = valor.replaceAll("[^0-9]", "");
        return normalizado.isBlank() ? null : normalizado;
    }

    private String normalizarBusquedaNombre(String valor) {
        if (valor == null || valor.trim().isBlank()) {
            throw new BusinessException("El nombre de busqueda es obligatorio");
        }

        return valor.trim();
    }

    private String normalizarTexto(String valor) {
        String sinAcentos = Normalizer.normalize(valor == null ? "" : valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return sinAcentos.toLowerCase();
    }

    private String normalizarTextoBusqueda(String valor) {
        String sinAcentos = Normalizer.normalize(valor == null ? "" : valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return sinAcentos.toLowerCase(Locale.forLanguageTag("es"));
    }

    private String escaparPatronLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private jakarta.persistence.criteria.Expression<String> normalizarIdentificacionSql(
            jakarta.persistence.criteria.Expression<String> identificacion,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder
    ) {
        return criteriaBuilder.function(
                "regexp_replace",
                String.class,
                criteriaBuilder.coalesce(identificacion, ""),
                criteriaBuilder.literal("[.\\s-]"),
                criteriaBuilder.literal(""),
                criteriaBuilder.literal("g")
        );
    }
}
