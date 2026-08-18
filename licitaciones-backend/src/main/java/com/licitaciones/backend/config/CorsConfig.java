package com.licitaciones.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Habilita CORS para que el frontend React (Vite en desarrollo, y el
 * deploy de produccion) pueda consumir la API. Los origenes permitidos
 * salen de {@link CorsProperties}: siempre incluye el de desarrollo
 * (app.cors.allowed-origin / CORS_ALLOWED_ORIGIN, por defecto
 * http://localhost:5173) y, si esta definido, tambien el de produccion
 * (app.cors.frontend-url / FRONTEND_URL, ej. el dominio de Vercel) — nunca
 * se pierde el de desarrollo por definir el de produccion.
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origenesPermitidos())
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    private String[] origenesPermitidos() {
        if (StringUtils.hasText(corsProperties.frontendUrl())) {
            return new String[] { corsProperties.allowedOrigin(), corsProperties.frontendUrl() };
        }
        return new String[] { corsProperties.allowedOrigin() };
    }
}
