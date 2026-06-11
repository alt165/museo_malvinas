package com.proveedores.report;

import java.util.List;

public record TabularReport<T>(
        String title,
        List<ReportFilter> filters,
        List<ReportColumn<T>> columns,
        List<T> rows
) {
}
