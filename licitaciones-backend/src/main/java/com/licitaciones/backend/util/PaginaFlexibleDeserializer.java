package com.licitaciones.backend.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * n8n casi siempre envia "pagina" como un unico entero
 * ({@code "pagina": 5}), pero en campos donde el dato extraido aparece
 * repetido a lo largo del documento a veces lo envia como un arreglo
 * ({@code "pagina": [17, 19]}). Con un campo declarado simplemente como
 * {@code Integer pagina}, ese segundo caso rompe la deserializacion del
 * array raiz completo con
 * {@code Cannot deserialize value of type java.lang.Integer from Array
 * value (token JsonToken.START_ARRAY)}, lo que RestTemplate reporta hacia
 * arriba como el generico "Error while extracting response for type
 * java.util.List&lt;N8nAnalisisResponseDTO&gt;" (502 Bad Gateway) sin dejar
 * ver cual campo fallo.
 *
 * Este deserializador acepta ambas formas y siempre expone un
 * {@code Integer}: si llega un numero lo usa tal cual; si llega un arreglo,
 * toma su primer elemento (la primera pagina donde se encontro el dato) para
 * no romper el contrato actual hacia el frontend, que renderiza "pagina"
 * como un unico numero (ver Resultado.jsx). Un arreglo vacio o un valor nulo
 * deserializan a {@code null}, igual que si la propiedad no viniera.
 *
 * No se usa para {@code CodigosRupDTO.pagina}: ahi el arreglo tiene un
 * significado distinto (una entrada por cada pagina donde aparecio algun
 * codigo RUP, sin relacion 1:1 con "valor") y por eso ese campo se modela
 * fielmente como {@code List<Integer>} en vez de normalizarse a un solo
 * numero.
 */
public class PaginaFlexibleDeserializer extends JsonDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode nodo = p.getCodec().readTree(p);
        if (nodo == null || nodo.isNull() || nodo.isMissingNode()) {
            return null;
        }
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
