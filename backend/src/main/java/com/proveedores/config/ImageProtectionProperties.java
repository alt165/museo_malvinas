package com.proveedores.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "app.images")
public record ImageProtectionProperties(
        PublicVersion publicVersion,
        Watermark watermark
) {
    public record PublicVersion(int maxSize, long cacheMaxAgeSeconds) {
    }

    public record Watermark(
            float opacity,
            double relativeWidth,
            double horizontalGap,
            double verticalGap,
            Resource resource
    ) {
    }
}
