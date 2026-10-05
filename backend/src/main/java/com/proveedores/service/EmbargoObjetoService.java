package com.proveedores.service;

import com.proveedores.dto.EmbargoObjetoRequestDTO;
import com.proveedores.dto.EmbargoObjetoResponseDTO;
import com.proveedores.entity.EmbargoObjeto;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.TipoOperacionAuditoria;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.EmbargoObjetoMapper;
import com.proveedores.report.PdfReportService;
import com.proveedores.report.ReportColumn;
import com.proveedores.report.ReportFilter;
import com.proveedores.report.ReportMetadata;
import com.proveedores.report.TabularReport;
import com.proveedores.repository.EmbargoObjetoRepository;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EmbargoObjetoService {

    private static final String INSTITUTION_NAME = "Museo de la Guerra de Malvinas";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmbargoObjetoRepository embargoObjetoRepository;
    private final ObjetoMuseoService objetoMuseoService;
    private final AuditoriaObjetoService auditoriaObjetoService;
    private final PdfReportService pdfReportService;

    public EmbargoObjetoService(
            EmbargoObjetoRepository embargoObjetoRepository,
            ObjetoMuseoService objetoMuseoService,
            AuditoriaObjetoService auditoriaObjetoService,
            PdfReportService pdfReportService
    ) {
        this.embargoObjetoRepository = embargoObjetoRepository;
        this.objetoMuseoService = objetoMuseoService;
        this.auditoriaObjetoService = auditoriaObjetoService;
        this.pdfReportService = pdfReportService;
    }

    @Transactional
    public EmbargoObjetoResponseDTO crear(EmbargoObjetoRequestDTO dto, String operador) {
        ObjetoMuseo objeto = objetoMuseoService.buscarObjetoActivo(dto.objetoMuseoId());
        if (embargoObjetoRepository.existsByObjetoMuseoIdAndFechaFinalizacionIsNullAndEliminadoFalse(objeto.getId())) {
            throw new BusinessException("El objeto ya tiene un embargo vigente");
        }
        LocalDate fechaInicio = dto.fechaInicio() == null ? com.proveedores.time.MuseoTime.today() : dto.fechaInicio();
        validarFechas(fechaInicio, dto.fechaFinalizacion());

        EmbargoObjeto embargo = new EmbargoObjeto();
        embargo.setObjetoMuseo(objeto);
        embargo.setFechaInicio(fechaInicio);
        embargo.setFechaFinalizacion(dto.fechaFinalizacion());
        embargo.setObservaciones(dto.observaciones());
        EmbargoObjeto saved = embargoObjetoRepository.save(embargo);

        auditoriaObjetoService.registrar(
                objeto,
                TipoOperacionAuditoria.MODIFICACION,
                "ALTA_EMBARGO",
                "Alta de embargo de objeto",
                "EMBARGO",
                null,
                snapshot(saved),
                operador
        );
        return EmbargoObjetoMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EmbargoObjetoResponseDTO> listar(boolean incluirHistoricos) {
        List<EmbargoObjeto> embargos = incluirHistoricos
                ? embargoObjetoRepository.findByEliminadoFalseOrderByFechaFinalizacionAscFechaInicioDescIdDesc()
                : embargoObjetoRepository.findByFechaFinalizacionIsNullAndEliminadoFalseOrderByFechaInicioDescIdDesc();
        return embargos.stream().map(EmbargoObjetoMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<EmbargoObjetoResponseDTO> buscar(String texto, boolean incluirHistoricos, Pageable pageable) {
        String filtro = StringUtils.hasText(texto) ? texto.trim() : null;
        int size = Math.min(Math.max(pageable.getPageSize(), 1), 50);
        Sort orden = incluirHistoricos
                ? Sort.by(Sort.Order.asc("fechaFinalizacion"), Sort.Order.desc("fechaInicio"), Sort.Order.desc("id"))
                : Sort.by(Sort.Order.desc("fechaInicio"), Sort.Order.desc("id"));
        Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), size, orden);
        Specification<EmbargoObjeto> specification = (root, query, criteriaBuilder) -> {
            Predicate filtrosEstado = criteriaBuilder.isFalse(root.get("eliminado"));
            if (!incluirHistoricos) {
                filtrosEstado = criteriaBuilder.and(filtrosEstado, criteriaBuilder.isNull(root.get("fechaFinalizacion")));
            }
            if (filtro == null) {
                return filtrosEstado;
            }

            Join<EmbargoObjeto, ObjetoMuseo> objeto = root.join("objetoMuseo", JoinType.INNER);
            String textoEscapado = escaparPatronLike(filtro).toLowerCase(Locale.ROOT);
            Expression<String> patron = criteriaBuilder.literal("%" + textoEscapado + "%");
            Predicate coincide = criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(objeto.get("numeroInventario")), patron, '\\'),
                    criteriaBuilder.like(criteriaBuilder.lower(objeto.get("denominacionObjeto")), patron, '\\')
            );
            return criteriaBuilder.and(filtrosEstado, coincide);
        };
        return embargoObjetoRepository.findAll(specification, pageRequest).map(EmbargoObjetoMapper::toResponse);
    }

    private String escaparPatronLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Transactional
    public EmbargoObjetoResponseDTO levantar(Long id, String operador) {
        EmbargoObjeto embargo = embargoObjetoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Embargo no encontrado"));
        if (embargo.getEliminado()) {
            throw new ResourceNotFoundException("Embargo no encontrado");
        }
        if (embargo.getFechaFinalizacion() != null) {
            return EmbargoObjetoMapper.toResponse(embargo);
        }

        Object anteriores = snapshot(embargo);
        embargo.setFechaFinalizacion(com.proveedores.time.MuseoTime.today());
        EmbargoObjeto saved = embargoObjetoRepository.save(embargo);

        auditoriaObjetoService.registrar(
                saved.getObjetoMuseo(),
                TipoOperacionAuditoria.MODIFICACION,
                "LEVANTAMIENTO_EMBARGO",
                "Levantamiento de embargo de objeto",
                "EMBARGO",
                anteriores,
                snapshot(saved),
                operador
        );
        return EmbargoObjetoMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public boolean tieneEmbargoVigente(Long objetoMuseoId) {
        return embargoObjetoRepository.existsByObjetoMuseoIdAndFechaFinalizacionIsNullAndEliminadoFalse(objetoMuseoId);
    }

    @Transactional(readOnly = true)
    public byte[] exportarVigentesPdf(String usuario) {
        List<EmbargoObjetoResponseDTO> embargos = listar(false);
        TabularReport<EmbargoObjetoResponseDTO> report = new TabularReport<>(
                "Objetos embargados",
                "Resumen del reporte",
                List.of(new ReportFilter("Estado", "Embargos vigentes")),
                columnasReporte(),
                embargos,
                "No hay embargos vigentes."
        );
        return pdfReportService.generate(report, new ReportMetadata(INSTITUTION_NAME, com.proveedores.time.MuseoTime.now(), usuario));
    }

    private void validarFechas(LocalDate fechaInicio, LocalDate fechaFinalizacion) {
        if (fechaFinalizacion != null && fechaFinalizacion.isBefore(fechaInicio)) {
            throw new BusinessException("La fecha de finalizacion no puede ser anterior a la fecha de inicio");
        }
    }

    private Object snapshot(EmbargoObjeto embargo) {
        return auditoriaObjetoService.mapOf(
                "embargoId", embargo.getId(),
                "objetoMuseoId", embargo.getObjetoMuseo().getId(),
                "numeroInventario", embargo.getObjetoMuseo().getNumeroInventario(),
                "fechaInicio", embargo.getFechaInicio(),
                "fechaFinalizacion", embargo.getFechaFinalizacion(),
                "estado", embargo.getFechaFinalizacion() == null ? "VIGENTE" : "LEVANTADO",
                "observaciones", embargo.getObservaciones()
        );
    }

    private List<ReportColumn<EmbargoObjetoResponseDTO>> columnasReporte() {
        return List.of(
                column("Número de inventario", 1.2f, EmbargoObjetoResponseDTO::numeroInventario),
                column("Denominación", 2.0f, EmbargoObjetoResponseDTO::denominacionObjeto),
                column("Fecha inicio", 1.0f, item -> fecha(item.fechaInicio())),
                column("Fecha finalización", 1.0f, item -> fecha(item.fechaFinalizacion())),
                column("Estado", 1.0f, EmbargoObjetoResponseDTO::estado),
                column("Observaciones", 2.0f, EmbargoObjetoResponseDTO::observaciones)
        );
    }

    private ReportColumn<EmbargoObjetoResponseDTO> column(String header, float width, Function<EmbargoObjetoResponseDTO, String> extractor) {
        return new ReportColumn<>(header, width, extractor);
    }

    private String fecha(LocalDate fecha) {
        return fecha == null ? null : DATE_FORMAT.format(fecha);
    }
}
