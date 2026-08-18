package com.licitaciones.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion del JWT emitido por {@code auth.JwtService} tras un login
 * exitoso. Se enlaza desde application.properties (prefijo "jwt"),
 * permitiendo sobreescritura via variables de entorno JWT_SECRET /
 * JWT_EXPIRATION.
 *
 * "secret" NUNCA debe quedar hardcodeado en un valor debil en produccion:
 * el default de application.properties es solo para desarrollo local: debe
 * sobreescribirse con JWT_SECRET en cualquier ambiente real.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long expirationMs
) {
}
