package com.licitaciones.backend.exception;

/**
 * Se lanza al intentar consultar el resultado del analisis (ficha tecnica,
 * documentacion y trazabilidad) de un proceso que aun no ha sido analizado.
 */
public class ResultadoNoDisponibleException extends RuntimeException {

    public ResultadoNoDisponibleException(String message) {
        super(message);
    }
}
