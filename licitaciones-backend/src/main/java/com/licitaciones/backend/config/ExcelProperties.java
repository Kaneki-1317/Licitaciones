package com.licitaciones.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades para la generacion del Excel final a partir de la plantilla
 * oficial. Se enlazan desde application.properties (prefijo "app.excel").
 */
@ConfigurationProperties(prefix = "app.excel")
public record ExcelProperties(
        String templatePath,
        String outputDir
) {
}
