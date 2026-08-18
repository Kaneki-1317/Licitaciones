package com.licitaciones.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta de {@code GET /api/health/version}: permite comprobar desde
 * fuera (navegador, curl, Postman) que instancia del backend esta
 * respondiendo realmente y que tan fresco es el codigo que tiene cargado,
 * sin tener que confiar en suposiciones sobre que proceso/JAR esta corriendo.
 *
 * "buildTime" NO es la fecha de arranque del proceso: es el ultimo
 * modificado real del .class de {@code N8nClient} tal como lo ve el
 * classloader en este momento (ver HealthController), asi que sirve para
 * demostrar si esta instancia tiene o no un cambio de codigo especifico,
 * independientemente de si corre desde el JAR empaquetado o desde
 * target/classes explotado (IDE / spring-boot-devtools).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VersionResponseDTO {

    private String application;
    private String version;
    private String buildTime;
    private String javaVersion;
    private String status;
}
