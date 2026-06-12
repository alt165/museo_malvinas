package com.proveedores.service;

import com.proveedores.dto.ColeccionObjetoResponseDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.report.ObjetoMuseoReportColumns;
import com.proveedores.report.PdfReportService;
import com.proveedores.report.ReportFilter;
import com.proveedores.report.ReportMetadata;
import com.proveedores.report.TabularReport;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ColeccionObjetoExportService {

    private static final String INSTITUTION_NAME = "Museo de la Guerra de Malvinas";

    private final ColeccionObjetoService coleccionObjetoService;
    private final PdfReportService pdfReportService;
    private final ObjetoMuseoReportColumns objetoMuseoReportColumns;

    public ColeccionObjetoExportService(
            ColeccionObjetoService coleccionObjetoService,
            PdfReportService pdfReportService,
            ObjetoMuseoReportColumns objetoMuseoReportColumns
    ) {
        this.coleccionObjetoService = coleccionObjetoService;
        this.pdfReportService = pdfReportService;
        this.objetoMuseoReportColumns = objetoMuseoReportColumns;
    }

    public byte[] exportarPdf(Long coleccionId, String usuario) {
        ColeccionObjetoResponseDTO coleccion = coleccionObjetoService.obtenerPorId(coleccionId);
        List<ObjetoMuseoResponseDTO> objetos = coleccionObjetoService.listarObjetos(coleccionId);
        TabularReport<ObjetoMuseoResponseDTO> report = new TabularReport<>(
                "Reporte de Colección",
                "Datos de la colección",
                datosColeccion(coleccion, objetos.size()),
                objetoMuseoReportColumns.columns(),
                objetos,
                "La colección no tiene objetos asociados."
        );
        return pdfReportService.generate(report, new ReportMetadata(INSTITUTION_NAME, LocalDateTime.now(), usuario));
    }

    private List<ReportFilter> datosColeccion(ColeccionObjetoResponseDTO coleccion, int cantidadObjetos) {
        List<ReportFilter> datos = new ArrayList<>();
        datos.add(new ReportFilter("Nombre de la colección", coleccion.nombre()));
        if (coleccion.descripcion() != null && !coleccion.descripcion().isBlank()) {
            datos.add(new ReportFilter("Descripción", coleccion.descripcion().trim()));
        }
        datos.add(new ReportFilter("Cantidad total de objetos incluidos", String.valueOf(cantidadObjetos)));
        return datos;
    }
}
