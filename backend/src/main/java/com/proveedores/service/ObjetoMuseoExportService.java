package com.proveedores.service;

import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.entity.CategoriaObjeto;
import com.proveedores.report.ObjetoMuseoReportColumns;
import com.proveedores.report.PdfReportService;
import com.proveedores.report.ReportFilter;
import com.proveedores.report.ReportMetadata;
import com.proveedores.report.TabularReport;
import com.proveedores.repository.CategoriaObjetoRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ObjetoMuseoExportService {

    private static final String INSTITUTION_NAME = "Museo de la Guerra de Malvinas";

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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
