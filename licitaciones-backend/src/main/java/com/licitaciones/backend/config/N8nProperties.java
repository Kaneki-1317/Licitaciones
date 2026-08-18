package com.licitaciones.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de conexion hacia n8n. Se enlazan desde application.properties
 * (prefijo "n8n"), permitiendo sobreescritura via variables de entorno.
 */
@ConfigurationProperties(prefix = "n8n")
public record N8nProperties(
        String baseUrl,
        String webhookAnalizar,
        int connectTimeoutMs,
        int readTimeoutMs
) {
}
