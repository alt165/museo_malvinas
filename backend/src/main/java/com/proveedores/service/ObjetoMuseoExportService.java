package com.proveedores.service;

import com.proveedores.dto.CategoriaObjetoResponseDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.entity.CategoriaObjeto;
import com.proveedores.report.PdfReportService;
import com.proveedores.report.ReportColumn;
import com.proveedores.report.ReportFilter;
import com.proveedores.report.ReportMetadata;
import com.proveedores.report.TabularReport;
import com.proveedores.repository.CategoriaObjetoRepository;
import java.time.LocalDate;
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
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ObjetoMuseoService objetoMuseoService;
    private final CategoriaObjetoRepository categoriaObjetoRepository;
    private final PdfReportService pdfReportService;

    public ObjetoMuseoExportService(
            ObjetoMuseoService objetoMuseoService,
            CategoriaObjetoRepository categoriaObjetoRepository,
            PdfReportService pdfReportService
    ) {
        this.objetoMuseoService = objetoMuseoService;
        this.categoriaObjetoRepository = categoriaObjetoRepository;
        this.pdfReportService = pdfReportService;
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
                columnas(),
                objetos
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

    private List<ReportColumn<ObjetoMuseoResponseDTO>> columnas() {
        return List.of(
                column("Número de inventario", 1.2f, ObjetoMuseoResponseDTO::numeroInventario),
                column("Nombre / denominación", 2.0f, ObjetoMuseoResponseDTO::denominacionObjeto),
                column("Categoría", 1.8f, this::categorias),
                column("Ubicación", 1.3f, ObjetoMuseoResponseDTO::ubicacionNombre),
                column("Estado de conservación", 1.1f, item -> enumText(item.estadoConservacion())),
                column("Depositante", 1.6f, ObjetoMuseoResponseDTO::depositanteNombre),
                column("Carácter de recepción", 1.2f, item -> enumText(item.caracterRecepcion())),
                column("Fecha de ingreso", 1.0f, item -> fecha(item.fechaIngreso()))
        );
    }

    private ReportColumn<ObjetoMuseoResponseDTO> column(String header, float width, Function<ObjetoMuseoResponseDTO, String> extractor) {
        return new ReportColumn<>(header, width, extractor);
    }

    private String categorias(ObjetoMuseoResponseDTO objeto) {
        if (objeto.categorias() == null || objeto.categorias().isEmpty()) {
            return null;
        }
        return objeto.categorias().stream()
                .map(CategoriaObjetoResponseDTO::nombre)
                .collect(Collectors.joining(", "));
    }

    private String fecha(LocalDate fecha) {
        return fecha == null ? null : DATE_FORMAT.format(fecha);
    }

    private String enumText(Enum<?> value) {
        return value == null ? null : value.name().replace('_', ' ');
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
