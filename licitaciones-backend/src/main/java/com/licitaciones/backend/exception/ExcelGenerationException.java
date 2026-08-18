package com.licitaciones.backend.exception;

/**
 * Se lanza cuando falla la lectura de la plantilla o la generacion del
 * Excel final del proceso.
 */
public class ExcelGenerationException extends RuntimeException {

    public ExcelGenerationException(String message) {
        super(message);
    }

    public ExcelGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
