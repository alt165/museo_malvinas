package com.proveedores.service;

import com.proveedores.dto.ComodatoPrestamoResponseDTO;
import com.proveedores.report.PdfReportService;
import com.proveedores.report.ReportColumn;
import com.proveedores.report.ReportFilter;
import com.proveedores.report.ReportMetadata;
import com.proveedores.report.TabularReport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class ComodatoPrestamoExportService {

    private static final String INSTITUTION_NAME = "Museo de la Guerra de Malvinas";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ComodatoPrestamoService comodatoPrestamoService;
    private final PdfReportService pdfReportService;

    public ComodatoPrestamoExportService(ComodatoPrestamoService comodatoPrestamoService, PdfReportService pdfReportService) {
        this.comodatoPrestamoService = comodatoPrestamoService;
        this.pdfReportService = pdfReportService;
    }

    public byte[] exportarPdf(String usuario) {
        List<ComodatoPrestamoResponseDTO> objetos = comodatoPrestamoService.listar();
        TabularReport<ComodatoPrestamoResponseDTO> report = new TabularReport<>(
                "Objetos en comodato o préstamo",
                "Resumen del reporte",
                List.of(
                        new ReportFilter("Caracteres incluidos", "COMODATO, PRESTAMO"),
                        new ReportFilter("Orden", "Fecha de vencimiento más próxima")
                ),
                columnas(),
                objetos,
                "No hay objetos activos recibidos como préstamo o comodato."
        );
        return pdfReportService.generate(report, new ReportMetadata(INSTITUTION_NAME, LocalDateTime.now(), usuario));
    }

    private List<ReportColumn<ComodatoPrestamoResponseDTO>> columnas() {
        return List.of(
                column("Número de inventario", 1.1f, ComodatoPrestamoResponseDTO::numeroInventario),
                column("Nombre / denominación", 1.7f, ComodatoPrestamoResponseDTO::denominacionObjeto),
                column("Depositante", 1.5f, ComodatoPrestamoResponseDTO::depositanteNombre),
                column("Carácter", 1.0f, item -> enumText(item.caracterRecepcion())),
                column("Fecha de ingreso", 1.0f, item -> fecha(item.fechaIngreso())),
                column("Fecha de vencimiento", 1.1f, item -> fecha(item.fechaVencimiento())),
                column("Días restantes", 1.0f, item -> diasRestantes(item.diasRestantes())),
                column("Estado", 1.2f, this::estado)
        );
    }

    private ReportColumn<ComodatoPrestamoResponseDTO> column(String header, float width, Function<ComodatoPrestamoResponseDTO, String> extractor) {
        return new ReportColumn<>(header, width, extractor);
    }

    private String estado(ComodatoPrestamoResponseDTO item) {
        if (item.fechaVencimiento() == null) {
            return "Sin fecha de vencimiento";
        }
        return switch (item.estadoVencimiento()) {
            case VENCIDO -> "Vencido";
            case PROXIMO_A_VENCER -> "Próximo a vencer";
            case VIGENTE -> "Vigente";
        };
    }

    private String diasRestantes(Long dias) {
        return dias == null ? "Sin fecha" : String.valueOf(dias);
    }

    private String fecha(LocalDate fecha) {
        return fecha == null ? null : DATE_FORMAT.format(fecha);
    }

    private String enumText(Enum<?> value) {
        return value == null ? null : value.name().replace("_", " ");
    }
}
