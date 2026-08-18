package com.licitaciones.backend.exception;

/**
 * Se lanza cuando se solicita un {@code ProcesoLicitacion} que no existe.
 */
public class ProcesoNotFoundException extends RuntimeException {

    public ProcesoNotFoundException(Long id) {
        super("No se encontro el proceso de licitacion con id: " + id);
    }
}
