package com.proveedores.report;

import java.util.function.Function;

public record ReportColumn<T>(
        String header,
        float width,
        Function<T, String> valueExtractor
) {
}
