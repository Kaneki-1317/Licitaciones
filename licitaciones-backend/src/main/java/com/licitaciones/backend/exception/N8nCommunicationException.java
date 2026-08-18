package com.licitaciones.backend.exception;

/**
 * Se lanza cuando falla la comunicacion (envio o recepcion) con n8n.
 */
public class N8nCommunicationException extends RuntimeException {

    public N8nCommunicationException(String message) {
        super(message);
    }

    public N8nCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
