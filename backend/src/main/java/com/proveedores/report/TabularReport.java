package com.proveedores.report;

import java.util.List;

public record TabularReport<T>(
        String title,
        String summaryTitle,
        List<ReportFilter> filters,
        List<ReportColumn<T>> columns,
        List<T> rows,
        String emptyMessage
) {
    public TabularReport(String title, List<ReportFilter> filters, List<ReportColumn<T>> columns, List<T> rows) {
        this(title, "Filtros aplicados", filters, columns, rows, "No hay registros para los filtros aplicados.");
    }

    public TabularReport(String title, String summaryTitle, List<ReportFilter> filters, List<ReportColumn<T>> columns, List<T> rows) {
        this(title, summaryTitle, filters, columns, rows, "No hay registros para los filtros aplicados.");
    }
}
