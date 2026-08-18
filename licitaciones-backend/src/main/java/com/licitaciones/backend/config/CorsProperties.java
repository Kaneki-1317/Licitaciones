package com.licitaciones.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Origen permitido para las peticiones del frontend React.
 * (prefijo "app.cors").
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        String allowedOrigin
) {
}
