package com.proveedores.service;

import com.proveedores.dto.DepositanteResponseDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.mapper.DepositanteMapper;
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
public class DepositanteExportService {

    private static final String INSTITUTION_NAME = "Museo de la Guerra de Malvinas";

    private final DepositanteService depositanteService;
    private final PdfReportService pdfReportService;
    private final ObjetoMuseoReportColumns objetoMuseoReportColumns;

    public DepositanteExportService(
            DepositanteService depositanteService,
            PdfReportService pdfReportService,
            ObjetoMuseoReportColumns objetoMuseoReportColumns
    ) {
        this.depositanteService = depositanteService;
        this.pdfReportService = pdfReportService;
        this.objetoMuseoReportColumns = objetoMuseoReportColumns;
    }

    public byte[] exportarObjetosPdf(Long depositanteId, String usuario) {
        DepositanteResponseDTO depositante = DepositanteMapper.toResponse(depositanteService.buscarActivo(depositanteId));
        List<ObjetoMuseoResponseDTO> objetos = depositanteService.listarObjetos(depositanteId);
        TabularReport<ObjetoMuseoResponseDTO> report = new TabularReport<>(
                "Reporte de objetos entregados por depositante",
                "Datos del depositante",
                datosDepositante(depositante, objetos.size()),
                objetoMuseoReportColumns.depositanteColumns(),
                objetos,
                "Este depositante no tiene objetos registrados."
        );
        return pdfReportService.generate(report, new ReportMetadata(INSTITUTION_NAME, LocalDateTime.now(), usuario));
    }

    private List<ReportFilter> datosDepositante(DepositanteResponseDTO depositante, int cantidadObjetos) {
        List<ReportFilter> datos = new ArrayList<>();
        datos.add(new ReportFilter("Nombre del depositante", depositante.nombre()));
        datos.add(new ReportFilter("Tipo de depositante", depositante.tipo() == null ? null : depositante.tipo().name()));
        if (depositante.dni() != null && !depositante.dni().isBlank()) {
            datos.add(new ReportFilter("DNI", depositante.dni()));
        }
        if (depositante.cuit() != null && !depositante.cuit().isBlank()) {
            datos.add(new ReportFilter("CUIT", depositante.cuit()));
        }
        datos.add(new ReportFilter("Cantidad total de objetos entregados", String.valueOf(cantidadObjetos)));
        return datos;
    }
}
