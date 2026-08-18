package com.licitaciones.backend.controller;

import com.licitaciones.backend.client.N8nClient;
import com.licitaciones.backend.dto.response.VersionResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Endpoint de diagnostico para responder, desde HTTP, la pregunta "¿que
 * instancia del backend me esta atendiendo y que tan fresco es su codigo?"
 * sin depender de suposiciones (PID, puerto, JAR) que solo se pueden
 * verificar desde la maquina donde corre el proceso.
 *
 * "buildTime" no es la hora de arranque del proceso (eso ya lo da
 * cualquier "uptime"): es el ultimo-modificado real del .class de
 * {@link N8nClient} tal como lo ve el classloader de ESTA instancia en
 * este momento. Sirve para demostrar si el fix de deserializacion de n8n
 * (aplicado justamente en N8nClient) esta o no cargado, sin importar si el
 * proceso corre desde el JAR empaquetado (mvn package) o desde
 * target/classes explotado (IDE / spring-boot-devtools).
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final DateTimeFormatter FORMATO_ISO = DateTimeFormatter.ISO_INSTANT;

    @Value("${spring.application.name}")
    private String nombreAplicacion;

    @GetMapping("/version")
    public ResponseEntity<VersionResponseDTO> version() {
        VersionResponseDTO body = new VersionResponseDTO(
                nombreAplicacion,
                versionEmpaquetada(),
                buildTimeDeN8nClient(),
                System.getProperty("java.version"),
                "UP");
        return ResponseEntity.ok(body);
    }

    /**
     * {@code Package#getImplementationVersion()} solo devuelve algo cuando
     * la clase se carga desde un JAR con manifiesto (mvn package); corriendo
     * desde target/classes explotado (IDE) siempre es null, caso en el que
     * se informa explicitamente para no sugerir una version falsa.
     */
    private String versionEmpaquetada() {
        String version = getClass().getPackage().getImplementationVersion();
        return version != null ? version : "0.0.1-SNAPSHOT (classes explotadas, sin JAR)";
    }

    private String buildTimeDeN8nClient() {
        try {
            URL recursoClase = N8nClient.class.getResource(N8nClient.class.getSimpleName() + ".class");
            if (recursoClase == null) {
                return "desconocido (no se encontro N8nClient.class en el classpath)";
            }
            URLConnection conexion = recursoClase.openConnection();
            long ultimaModificacion = conexion.getLastModified();
            if (ultimaModificacion <= 0) {
                return "desconocido (el classloader no reporto fecha de modificacion)";
            }
            return FORMATO_ISO.format(Instant.ofEpochMilli(ultimaModificacion).atZone(ZoneId.systemDefault()));
        } catch (Exception ex) {
            return "desconocido (" + ex.getMessage() + ")";
        }
    }
}
