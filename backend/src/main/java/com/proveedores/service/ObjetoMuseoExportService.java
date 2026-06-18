package com.proveedores.service;

import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.entity.CategoriaObjeto;
import com.proveedores.report.ObjetoMuseoReportColumns;
import com.proveedores.report.PdfReportService;
import com.proveedores.report.ReportColumn;
import com.proveedores.report.ReportFilter;
import com.proveedores.report.ReportMetadata;
import com.proveedores.report.TabularReport;
import com.proveedores.repository.CategoriaObjetoRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ObjetoMuseoExportService {

    private static final String INSTITUTION_NAME = "Museo de la Guerra de Malvinas";
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ObjetoMuseoService objetoMuseoService;
    private final CategoriaObjetoRepository categoriaObjetoRepository;
    private final PdfReportService pdfReportService;
    private final ObjetoMuseoReportColumns objetoMuseoReportColumns;

    public ObjetoMuseoExportService(
            ObjetoMuseoService objetoMuseoService,
            CategoriaObjetoRepository categoriaObjetoRepository,
            PdfReportService pdfReportService,
            ObjetoMuseoReportColumns objetoMuseoReportColumns
    ) {
        this.objetoMuseoService = objetoMuseoService;
        this.categoriaObjetoRepository = categoriaObjetoRepository;
        this.pdfReportService = pdfReportService;
        this.objetoMuseoReportColumns = objetoMuseoReportColumns;
    }

    public byte[] exportarListadoPdf(
            String nombre,
            String numeroInventario,
            List<Long> categoriaIds,
            Sort sort,
            String usuario
    ) {
        List<ObjetoMuseoResponseDTO> objetos = objetoMuseoService.buscarParaExportacion(nombre, numeroInventario, categoriaIds, sort);
        TabularReport<ObjetoMuseoResponseDTO> report = new TabularReport<>(
                "Listado de Objetos",
                filtros(nombre, numeroInventario, categoriaIds),
                objetoMuseoReportColumns.columns(),
                objetos
        );
        return pdfReportService.generate(report, new ReportMetadata(INSTITUTION_NAME, LocalDateTime.now(), usuario));
    }

    public byte[] exportarPendientesCompletarPdf(Sort sort, String usuario) {
        List<ObjetoMuseoResponseDTO> objetos = objetoMuseoService.listarPendientesCompletarParaExportacion(sort);
        TabularReport<ObjetoMuseoResponseDTO> report = new TabularReport<>(
                "Objetos pendientes de completar",
                "Resumen del reporte",
                List.of(new ReportFilter("Tipo de reporte", "Objetos creados por alta rápida sin ficha completa")),
                columnasPendientes(),
                objetos,
                "No hay objetos de alta rápida pendientes de completar."
        );
        return pdfReportService.generate(report, new ReportMetadata(INSTITUTION_NAME, LocalDateTime.now(), usuario));
    }

    private List<ReportFilter> filtros(String nombre, String numeroInventario, List<Long> categoriaIds) {
        List<ReportFilter> filtros = new ArrayList<>();
        if (hasText(nombre)) {
            filtros.add(new ReportFilter("Nombre / denominación", nombre.trim()));
        }
        if (hasText(numeroInventario)) {
            filtros.add(new ReportFilter("Número de inventario", numeroInventario.trim()));
        }
        List<Long> ids = categoriaIds == null ? List.of() : categoriaIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (!ids.isEmpty()) {
            filtros.add(new ReportFilter("Categorías", nombresCategorias(ids)));
        }
        return filtros;
    }

    private String nombresCategorias(List<Long> categoriaIds) {
        Map<Long, String> nombres = categoriaObjetoRepository.findAllById(categoriaIds).stream()
                .filter(categoria -> !Boolean.TRUE.equals(categoria.getEliminado()))
                .collect(Collectors.toMap(CategoriaObjeto::getId, CategoriaObjeto::getNombre));
        return categoriaIds.stream()
                .map(id -> nombres.getOrDefault(id, "ID " + id))
                .collect(Collectors.joining(", "));
    }

    private List<ReportColumn<ObjetoMuseoResponseDTO>> columnasPendientes() {
        return List.of(
                column("Número de inventario", 1.1f, ObjetoMuseoResponseDTO::numeroInventario),
                column("Nombre / denominación", 1.7f, ObjetoMuseoResponseDTO::denominacionObjeto),
                column("Fecha de alta rápida", 1.2f, item -> fechaHora(item.fechaCargaRapida())),
                column("Usuario alta", 1.1f, ObjetoMuseoResponseDTO::cargaRapidaPor),
                column("Depositante", 1.4f, ObjetoMuseoResponseDTO::depositanteNombre),
                column("Ubicación actual", 1.2f, ObjetoMuseoResponseDTO::ubicacionNombre),
                column("Estado conservación", 1.1f, item -> enumText(item.estadoConservacion())),
                column("Campos pendientes", 2.2f, this::camposPendientes)
        );
    }

    private ReportColumn<ObjetoMuseoResponseDTO> column(String header, float width, Function<ObjetoMuseoResponseDTO, String> extractor) {
        return new ReportColumn<>(header, width, extractor);
    }

    private String camposPendientes(ObjetoMuseoResponseDTO objeto) {
        List<String> pendientes = new ArrayList<>();
        if (!hasText(objeto.denominacionObjeto())) {
            pendientes.add("Nombre / denominación");
        }
        if (!hasText(objeto.descripcionTecnica())) {
            pendientes.add("Descripción técnica");
        }
        if (!hasText(objeto.materiales())) {
            pendientes.add("Materiales");
        }
        if (!hasText(objeto.dimensiones())) {
            pendientes.add("Dimensiones");
        }
        if (objeto.estadoConservacion() == null) {
            pendientes.add("Estado de conservación");
        }
        if (objeto.categorias() == null || objeto.categorias().isEmpty()) {
            pendientes.add("Categoría");
        }
        return pendientes.isEmpty() ? "Ficha pendiente de revisión" : String.join(", ", pendientes);
    }

    private String fechaHora(LocalDateTime fecha) {
        return fecha == null ? null : DATE_TIME_FORMAT.format(fecha);
    }

    private String enumText(Enum<?> value) {
        return value == null ? null : value.name().replace("_", " ");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
