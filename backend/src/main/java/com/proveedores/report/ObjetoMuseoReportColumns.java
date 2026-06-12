package com.proveedores.report;

import com.proveedores.dto.CategoriaObjetoResponseDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ObjetoMuseoReportColumns {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public List<ReportColumn<ObjetoMuseoResponseDTO>> columns() {
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

    public List<ReportColumn<ObjetoMuseoResponseDTO>> depositanteColumns() {
        return List.of(
                column("Número de inventario", 1.2f, ObjetoMuseoResponseDTO::numeroInventario),
                column("Nombre / denominación", 2.0f, ObjetoMuseoResponseDTO::denominacionObjeto),
                column("Categoría", 1.8f, this::categorias),
                column("Ubicación", 1.4f, ObjetoMuseoResponseDTO::ubicacionNombre),
                column("Estado de conservación", 1.2f, item -> enumText(item.estadoConservacion())),
                column("Carácter de recepción", 1.2f, item -> enumText(item.caracterRecepcion())),
                column("Fecha de ingreso", 1.0f, item -> fecha(item.fechaIngreso())),
                column("Fecha de vencimiento", 1.0f, item -> fecha(item.fechaVencimiento()))
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
}
