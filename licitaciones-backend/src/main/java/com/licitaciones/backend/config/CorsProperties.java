package com.licitaciones.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Origenes permitidos para las peticiones del frontend React.
 * (prefijo "app.cors").
 *
 * "allowedOrigin" es el origen de desarrollo (Vite local, ver
 * CORS_ALLOWED_ORIGIN). "frontendUrl" es opcional y representa el dominio
 * de produccion (ej. el deploy de Vercel, ver FRONTEND_URL): si se define,
 * CorsConfig lo agrega COMO SEGUNDO origen permitido, sin reemplazar el de
 * desarrollo (asi el mismo backend sirve a localhost y a produccion a la
 * vez, sin tener que tocar esta configuracion al pasar de un ambiente a
 * otro).
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        String allowedOrigin,
        String frontendUrl
) {
}
