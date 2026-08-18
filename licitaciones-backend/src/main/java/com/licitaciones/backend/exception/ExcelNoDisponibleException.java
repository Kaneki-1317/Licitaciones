package com.licitaciones.backend.exception;

/**
 * Se lanza al intentar descargar el Excel de un proceso que aun no ha
 * finalizado su analisis (o cuyo archivo generado no existe en disco).
 */
public class ExcelNoDisponibleException extends RuntimeException {

    public ExcelNoDisponibleException(String message) {
        super(message);
    }
}
