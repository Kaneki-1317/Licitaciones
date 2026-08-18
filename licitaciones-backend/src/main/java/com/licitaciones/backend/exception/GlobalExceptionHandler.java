package com.licitaciones.backend.exception;

import com.licitaciones.backend.dto.response.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Manejo centralizado de excepciones para toda la API REST.
 * Traduce las excepciones de negocio/infraestructura a respuestas HTTP
 * consistentes, sin exponer detalles internos innecesarios.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ProcesoNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleProcesoNotFound(ProcesoNotFoundException ex,
                                                                    HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ExcelNoDisponibleException.class)
    public ResponseEntity<ErrorResponseDTO> handleExcelNoDisponible(ExcelNoDisponibleException ex,
                                                                      HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ResultadoNoDisponibleException.class)
    public ResponseEntity<ErrorResponseDTO> handleResultadoNoDisponible(ResultadoNoDisponibleException ex,
                                                                          HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(N8nCommunicationException.class)
    public ResponseEntity<ErrorResponseDTO> handleN8nCommunication(N8nCommunicationException ex,
                                                                      HttpServletRequest request) {
        log.error("Error de comunicacion con n8n: {}", ex.getMessage(), ex);
        return construir(HttpStatus.BAD_GATEWAY, "No fue posible comunicarse con el servicio de analisis (n8n). "
                + ex.getMessage(), request, null);
    }

    @ExceptionHandler(ExcelGenerationException.class)
    public ResponseEntity<ErrorResponseDTO> handleExcelGeneration(ExcelGenerationException ex,
                                                                     HttpServletRequest request) {
        log.error("Error generando el Excel final: {}", ex.getMessage(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(MethodArgumentNotValidException ex,
                                                                HttpServletRequest request) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        return construir(HttpStatus.BAD_REQUEST, "Datos invalidos en la solicitud", request, detalles);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDTO> handleMaxUploadSize(MaxUploadSizeExceededException ex,
                                                                    HttpServletRequest request) {
        return construir(HttpStatus.PAYLOAD_TOO_LARGE,
                "Los archivos enviados superan el tamano maximo permitido", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado: {}", ex.getMessage(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error inesperado en el servidor",
                request, null);
    }

    private ResponseEntity<ErrorResponseDTO> construir(HttpStatus status, String mensaje,
                                                          HttpServletRequest request, List<String> detalles) {
        ErrorResponseDTO body = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .mensaje(mensaje)
                .path(request.getRequestURI())
                .detalles(detalles)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
