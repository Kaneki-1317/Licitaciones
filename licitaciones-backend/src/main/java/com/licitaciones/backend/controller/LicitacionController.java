package com.licitaciones.backend.controller;

import com.licitaciones.backend.dto.request.CrearProcesoRequestDTO;
import com.licitaciones.backend.dto.response.EstadoResponseDTO;
import com.licitaciones.backend.dto.response.N8nAnalisisResponseDTO;
import com.licitaciones.backend.dto.response.ProcesoResponseDTO;
import com.licitaciones.backend.service.ExcelService;
import com.licitaciones.backend.service.LicitacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Expone la API REST consumida por el frontend React. No contiene logica
 * de negocio: toda la orquestacion vive en {@link LicitacionService}.
 */
@RestController
@RequestMapping("/api/licitaciones")
@RequiredArgsConstructor
public class LicitacionController {

    private final LicitacionService licitacionService;

    /** Crea un nuevo proceso de licitacion en estado CREADO. */
    @PostMapping
    public ResponseEntity<ProcesoResponseDTO> crearProceso(@Valid @RequestBody CrearProcesoRequestDTO request) {
        ProcesoResponseDTO creado = licitacionService.crearProceso(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /** Lista el historial de procesos registrados. */
    @GetMapping
    public ResponseEntity<List<ProcesoResponseDTO>> listarProcesos() {
        return ResponseEntity.ok(licitacionService.listarProcesos());
    }

    /** Consulta el detalle de un proceso puntual. */
    @GetMapping("/{id}")
    public ResponseEntity<ProcesoResponseDTO> consultarProceso(@PathVariable Long id) {
        return ResponseEntity.ok(licitacionService.consultarProceso(id));
    }

    /**
     * Inicia el analisis del proceso: envia los documentos seleccionados
     * por el usuario a n8n, guarda el resultado y genera el Excel final.
     */
    @PostMapping(value = "/{id}/analizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProcesoResponseDTO> analizar(
            @PathVariable Long id,
            @RequestParam(value = "documentos", required = false) List<MultipartFile> documentos) {
        ProcesoResponseDTO resultado = licitacionService.iniciarAnalisis(id, documentos);
        return ResponseEntity.ok(resultado);
    }

    /** Consulta el estado actual del proceso (CREADO, ANALIZANDO, COMPLETADO, ERROR). */
    @GetMapping("/{id}/estado")
    public ResponseEntity<EstadoResponseDTO> consultarEstado(@PathVariable Long id) {
        return ResponseEntity.ok(licitacionService.consultarEstado(id));
    }

    /**
     * Consulta el resultado del ultimo analisis del proceso (ficha tecnica,
     * documentacion y trazabilidad de fuentes), sin necesidad de repetir el
     * analisis. Devuelve 409 si el proceso aun no ha sido analizado.
     */
    @GetMapping("/{id}/resultado")
    public ResponseEntity<N8nAnalisisResponseDTO> consultarResultado(@PathVariable Long id) {
        return ResponseEntity.ok(licitacionService.consultarResultado(id));
    }

    /** Descarga el archivo Excel final generado para el proceso. */
    @GetMapping("/{id}/excel")
    public ResponseEntity<Resource> descargarExcel(@PathVariable Long id) {
        Resource archivo = licitacionService.descargarExcel(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + ExcelService.NOMBRE_ARCHIVO_FINAL + "\"")
                .body(archivo);
    }
}
