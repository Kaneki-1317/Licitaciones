package com.licitaciones.backend.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.licitaciones.backend.dto.response.PolizaDTO;

import java.io.IOException;

/**
 * Cada poliza dentro de data.polizas puede llegar de dos formas segun la
 * version del flujo de n8n:
 *
 * <ul>
 *   <li>Contrato viejo (solo el porcentaje, sin trazabilidad):
 *       {@code "seriedad": 10 }</li>
 *   <li>Contrato nuevo (porcentaje + archivo + pagina donde se encontro):
 *       {@code "seriedad": { "valor": 10, "archivo": "...", "pagina": 42 } }</li>
 * </ul>
 *
 * Este deserializador acepta ambas formas y siempre expone un
 * {@link PolizaDTO}: si llega un numero "pelado" lo mapea a {@code valor},
 * dejando {@code archivo}/{@code pagina} en {@code null} (nunca se inventan);
 * si llega un objeto, lee sus tres campos tal cual, aceptando "pagina" como
 * numero o como arreglo (mismo criterio que {@link PaginaFlexibleDeserializer},
 * toma el primer elemento). 0 es un valor valido de "valor" y se preserva
 * (no se confunde con dato ausente).
 */
public class PolizaFlexibleDeserializer extends JsonDeserializer<PolizaDTO> {

    @Override
    public PolizaDTO deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode nodo = p.getCodec().readTree(p);
        if (nodo == null || nodo.isNull() || nodo.isMissingNode()) {
            return null;
        }

        if (nodo.isObject()) {
            Integer valor = extraerEntero(nodo.get("valor"));
            String archivo = nodo.hasNonNull("archivo") ? nodo.get("archivo").asText() : null;
            Integer pagina = extraerPagina(nodo.get("pagina"));
            return new PolizaDTO(valor, archivo, pagina);
        }

        // Contrato viejo: la poliza es directamente el numero del porcentaje.
        Integer valor = extraerEntero(nodo);
        return valor == null ? null : new PolizaDTO(valor, null, null);
    }

    private Integer extraerEntero(JsonNode nodo) {
        if (nodo == null || nodo.isNull() || nodo.isMissingNode()) return null;
        return nodo.canConvertToInt() ? nodo.intValue() : null;
    }

    private Integer extraerPagina(JsonNode nodo) {
        if (nodo == null || nodo.isNull() || nodo.isMissingNode()) return null;
        if (nodo.isArray()) {
            for (JsonNode elemento : nodo) {
                if (elemento.canConvertToInt()) {
                    return elemento.intValue();
                }
            }
            return null;
        }
        return nodo.canConvertToInt() ? nodo.intValue() : null;
    }
}
