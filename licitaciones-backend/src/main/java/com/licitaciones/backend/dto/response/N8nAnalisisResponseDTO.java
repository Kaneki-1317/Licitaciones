package com.licitaciones.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Estructura raiz del JSON devuelto por el flujo de n8n al finalizar el
 * analisis de los documentos del proceso:
 * {@code { "success": true, "data": { "fichaTecnica": {...} } } }.
 *
 * Este mismo DTO se reutiliza como respuesta de
 * {@code GET /api/licitaciones/{id}/resultado}: es el contrato REST final
 * y estable que consume el frontend.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class N8nAnalisisResponseDTO {

    private Boolean success;
    private AnalisisDataDTO data;
}
