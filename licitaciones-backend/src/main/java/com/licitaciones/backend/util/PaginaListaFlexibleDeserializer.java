package com.licitaciones.backend.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Variante de {@link PaginaFlexibleDeserializer} que, en vez de quedarse con
 * un unico numero, CONSERVA todas las paginas cuando n8n envia un arreglo
 * ({@code "pagina": [7, 15]}). Se usa donde perder una pagina adicional
 * significa perder trazabilidad real (documentacion.*.pagina,
 * polizas.paginasConsultadas), a diferencia de los campos donde el contrato
 * historico siempre asumio "una sola pagina por campo" (fichaTecnica via
 * {@link com.licitaciones.backend.dto.response.CampoTrazableDTO}, perfiles),
 * que siguen usando {@link PaginaFlexibleDeserializer} sin cambios — este
 * deserializador nuevo NO los reemplaza, se agrega solo donde hacia falta.
 *
 * Acepta:
 * <ul>
 *   <li>numero simple ({@code "pagina": 15}) -&gt; lista de un elemento, {@code [15]}</li>
 *   <li>arreglo de numeros ({@code "pagina": [7, 15]}) -&gt; se conservan TODOS
 *       los elementos numericos, {@code [7, 15]} (se descartan silenciosamente
 *       los elementos que no sean numericos, sin fallar la deserializacion)</li>
 *   <li>null, propiedad ausente, o arreglo vacio -&gt; {@code null} (nunca se
 *       inventa una pagina que no vino en el JSON)</li>
 * </ul>
 */
public class PaginaListaFlexibleDeserializer extends JsonDeserializer<List<Integer>> {

    @Override
    public List<Integer> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode nodo = p.getCodec().readTree(p);
        if (nodo == null || nodo.isNull() || nodo.isMissingNode()) {
            return null;
        }

        if (nodo.isArray()) {
            List<Integer> paginas = new ArrayList<>();
            for (JsonNode elemento : nodo) {
                if (elemento.canConvertToInt()) {
                    paginas.add(elemento.intValue());
                }
            }
            return paginas.isEmpty() ? null : paginas;
        }

        if (nodo.canConvertToInt()) {
            List<Integer> paginas = new ArrayList<>();
            paginas.add(nodo.intValue());
            return paginas;
        }

        return null;
    }
}
