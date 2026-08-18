package com.licitaciones.backend.service;

import com.licitaciones.backend.client.N8nClient;
import com.licitaciones.backend.dto.response.N8nAnalisisResponseDTO;
import com.licitaciones.backend.exception.N8nCommunicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Orquesta la comunicacion con n8n: delega el transporte HTTP en
 * {@link N8nClient} y aplica las reglas de negocio propias del backend
 * (logging, validaciones minimas y traduccion de errores).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class N8nService {

    private final N8nClient n8nClient;

    /**
     * Solicita a n8n el analisis de los documentos de un proceso y devuelve
     * el JSON estructurado ya resultante (ficha tecnica, documentacion y
     * trazabilidad de fuentes).
     */
    public N8nAnalisisResponseDTO analizarDocumentos(Long procesoId, List<MultipartFile> documentos) {
        if (documentos == null || documentos.isEmpty()) {
            log.warn("Iniciando analisis del proceso {} sin documentos adjuntos", procesoId);
        }

        try {
            N8nAnalisisResponseDTO resultado = n8nClient.enviarDocumentosParaAnalisis(procesoId, documentos);
            log.info("Analisis recibido de n8n para el proceso {}", procesoId);
            return resultado;
        } catch (N8nCommunicationException ex) {
            log.error("n8n no pudo procesar el proceso {}: {}", procesoId, ex.getMessage());
            throw ex;
        }
    }
}
