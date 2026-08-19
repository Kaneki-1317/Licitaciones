package com.licitaciones.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.licitaciones.backend.dto.request.CrearProcesoRequestDTO;
import com.licitaciones.backend.dto.response.EstadoResponseDTO;
import com.licitaciones.backend.dto.response.N8nAnalisisResponseDTO;
import com.licitaciones.backend.dto.response.ProcesoResponseDTO;
import com.licitaciones.backend.entity.EstadoProceso;
import com.licitaciones.backend.entity.ProcesoLicitacion;
import com.licitaciones.backend.entity.ResultadoAnalisis;
import com.licitaciones.backend.exception.ExcelNoDisponibleException;
import com.licitaciones.backend.exception.ProcesoNotFoundException;
import com.licitaciones.backend.exception.ResultadoNoDisponibleException;
import com.licitaciones.backend.repository.ProcesoLicitacionRepository;
import com.licitaciones.backend.repository.ResultadoAnalisisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio principal que coordina todo el ciclo de vida de un proceso de
 * licitacion: creacion, disparo del analisis en n8n, persistencia del
 * resultado, generacion del Excel final y actualizacion de estados.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LicitacionService {

    private final ProcesoLicitacionRepository procesoRepository;
    private final ResultadoAnalisisRepository resultadoRepository;
    private final N8nService n8nService;
    private final ExcelService excelService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ProcesoResponseDTO crearProceso(CrearProcesoRequestDTO request) {
        ProcesoLicitacion proceso = ProcesoLicitacion.builder()
                .nombreProceso(request.getNombreProceso())
                .estado(EstadoProceso.CREADO)
                .build();

        proceso = procesoRepository.save(proceso);
        log.info("Proceso de licitacion creado con id {}", proceso.getId());

        return mapearAResponse(proceso);
    }

    /**
     * Ejecuta el flujo completo de analisis: ANALIZANDO -> n8n -> guardar
     * resultado -> generar Excel -> COMPLETADO. Si algo falla, el proceso
     * queda marcado en estado ERROR y se propaga la excepcion original
     * (el {@code GlobalExceptionHandler} la traduce a la respuesta HTTP).
     */
    @Transactional
    public ProcesoResponseDTO iniciarAnalisis(Long procesoId, List<MultipartFile> documentos) {
        ProcesoLicitacion proceso = obtenerProcesoOrFail(procesoId);

        proceso.setEstado(EstadoProceso.ANALIZANDO);
        procesoRepository.save(proceso);

        try {
            N8nAnalisisResponseDTO resultado = n8nService.analizarDocumentos(procesoId, documentos);
            guardarResultado(procesoId, resultado);

            String rutaExcel = excelService.generarExcel(procesoId, resultado);

            proceso.setArchivoGenerado(rutaExcel);
            proceso.setEstado(EstadoProceso.COMPLETADO);
            proceso.setFechaFinalizacion(LocalDateTime.now());
            procesoRepository.save(proceso);

            log.info("Proceso {} completado correctamente", procesoId);
            return mapearAResponse(proceso);

        } catch (RuntimeException ex) {
            proceso.setEstado(EstadoProceso.ERROR);
            procesoRepository.save(proceso);
            log.error("El analisis del proceso {} termino en error: {}", procesoId, ex.getMessage());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public EstadoResponseDTO consultarEstado(Long procesoId) {
        ProcesoLicitacion proceso = obtenerProcesoOrFail(procesoId);
        return EstadoResponseDTO.builder().estado(proceso.getEstado()).build();
    }

    @Transactional(readOnly = true)
    public ProcesoResponseDTO consultarProceso(Long procesoId) {
        return mapearAResponse(obtenerProcesoOrFail(procesoId));
    }

    @Transactional(readOnly = true)
    public List<ProcesoResponseDTO> listarProcesos() {
        return procesoRepository.findAll().stream()
                .map(this::mapearAResponse)
                .toList();
    }

    /**
     * Consulta el ultimo resultado de analisis persistido para un proceso
     * (ficha tecnica, documentacion y trazabilidad de fuentes). Permite
     * recuperar esta informacion aunque el usuario recargue la pagina o
     * vuelva a consultar el proceso mas tarde.
     */
    @Transactional(readOnly = true)
    public N8nAnalisisResponseDTO consultarResultado(Long procesoId) {
        obtenerProcesoOrFail(procesoId);
        return obtenerResultadoAnalisis(procesoId);
    }

    /**
     * Descarga el Excel generado. El disco del contenedor (Render, sin un
     * volumen persistente adjunto) no sobrevive a un redeploy ni a que el
     * servicio se "duerma" y despierte de nuevo -- se pierde cualquier
     * archivo escrito previamente aunque el proceso siga marcado
     * COMPLETADO en la base de datos. Como el resultado de n8n si queda
     * persistido (tabla resultado_analisis), si el archivo no esta en
     * disco lo regeneramos al vuelo desde ese resultado en vez de fallar:
     * generarExcel es determinista (misma plantilla + mismo JSON = mismo
     * archivo), asi que no hace falta volver a llamar a n8n.
     */
    @Transactional(readOnly = true)
    public Resource descargarExcel(Long procesoId) {
        ProcesoLicitacion proceso = obtenerProcesoOrFail(procesoId);

        if (proceso.getEstado() != EstadoProceso.COMPLETADO || proceso.getArchivoGenerado() == null) {
            throw new ExcelNoDisponibleException(
                    "El Excel del proceso " + procesoId + " aun no esta disponible (estado actual: "
                            + proceso.getEstado() + ")");
        }

        Path archivo = Path.of(proceso.getArchivoGenerado());
        if (!Files.exists(archivo)) {
            log.warn("Excel del proceso {} no esta en disco (posible reinicio/redeploy del contenedor); "
                    + "regenerando desde el resultado de analisis guardado", procesoId);
            N8nAnalisisResponseDTO resultado = obtenerResultadoAnalisis(procesoId);
            String rutaExcel = excelService.generarExcel(procesoId, resultado);
            archivo = Path.of(rutaExcel);
        }
        return new FileSystemResource(archivo);
    }

    private N8nAnalisisResponseDTO obtenerResultadoAnalisis(Long procesoId) {
        ResultadoAnalisis resultadoAnalisis = resultadoRepository
                .findTopByProcesoIdOrderByFechaCreacionDesc(procesoId)
                .orElseThrow(() -> new ResultadoNoDisponibleException(
                        "El proceso " + procesoId + " aun no tiene un resultado de analisis disponible"));

        try {
            return objectMapper.readValue(resultadoAnalisis.getJsonResultado(), N8nAnalisisResponseDTO.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "No fue posible deserializar el resultado del analisis del proceso " + procesoId, ex);
        }
    }

    private void guardarResultado(Long procesoId, N8nAnalisisResponseDTO resultado) {
        try {
            String json = objectMapper.writeValueAsString(resultado);
            ResultadoAnalisis resultadoAnalisis = ResultadoAnalisis.builder()
                    .procesoId(procesoId)
                    .jsonResultado(json)
                    .build();
            resultadoRepository.save(resultadoAnalisis);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No fue posible serializar el resultado del analisis", ex);
        }
    }

    private ProcesoLicitacion obtenerProcesoOrFail(Long procesoId) {
        return procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ProcesoNotFoundException(procesoId));
    }

    private ProcesoResponseDTO mapearAResponse(ProcesoLicitacion proceso) {
        return ProcesoResponseDTO.builder()
                .id(proceso.getId())
                .nombreProceso(proceso.getNombreProceso())
                .estado(proceso.getEstado())
                .fechaCreacion(proceso.getFechaCreacion())
                .fechaFinalizacion(proceso.getFechaFinalizacion())
                .archivoGenerado(proceso.getArchivoGenerado())
                .build();
    }
}
