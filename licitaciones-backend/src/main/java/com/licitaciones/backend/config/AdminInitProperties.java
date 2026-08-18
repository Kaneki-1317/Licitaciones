package com.licitaciones.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciales del usuario administrador inicial, usadas UNICAMENTE por
 * {@link AdminUsuarioInitializer} al arrancar la aplicacion. Se enlazan
 * desde application.properties (prefijo "app.admin"), pensadas para venir
 * de las variables de entorno ADMIN_USERNAME / ADMIN_PASSWORD: no hay
 * password por defecto en el codigo fuente (ver AdminUsuarioInitializer,
 * que se abstiene de crear el admin si password llega vacio).
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminInitProperties(
        String username,
        String password
) {
}
