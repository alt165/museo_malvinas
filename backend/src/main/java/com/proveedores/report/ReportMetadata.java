package com.proveedores.report;

import java.time.LocalDateTime;

public record ReportMetadata(
        String institutionName,
        LocalDateTime generatedAt,
        String generatedBy
) {
}
