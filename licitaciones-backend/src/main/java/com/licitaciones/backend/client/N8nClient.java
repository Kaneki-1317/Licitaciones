package com.licitaciones.backend.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.licitaciones.backend.config.N8nProperties;
import com.licitaciones.backend.dto.response.N8nAnalisisResponseDTO;
import com.licitaciones.backend.exception.N8nCommunicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Cliente HTTP de bajo nivel para comunicarse con n8n. Se encarga
 * exclusivamente del transporte (armar la peticion multipart y llamar al
 * webhook); la logica de negocio/orquestacion vive en
 * {@code service.N8nService}.
 *
 * La llamada se hace en DOS pasos deliberadamente (en vez de un unico
 * {@code exchange(..., ParameterizedTypeReference<List<N8nAnalisisResponseDTO>>)})
 * para poder distinguir, ante cualquier falla, dos causas que hasta ahora
 * quedaban indistinguibles detras del mismo 502 "Error while extracting
 * response for type [...]":
 *
 * <ol>
 *   <li>n8n no respondio, respondio con error HTTP, o hubo un problema de
 *       red/timeout -&gt; falla en el paso 1 (transporte), antes de que exista
 *       ningun cuerpo que deserializar.</li>
 *   <li>n8n respondio 200 pero el JSON recibido no coincide con el contrato
 *       -&gt; falla en el paso 2 (formato), y en ese caso el cuerpo crudo ya
 *       quedo logueado ANTES de intentar deserializarlo, para poder
 *       inspeccionarlo aunque la deserializacion falle.</li>
 * </ol>
 *
 * <p><b>Normalizacion ARRAY/OBJECT:</b> el contrato "oficial" de n8n es un
 * array de un elemento ({@code [ { "success":..., "data":... } ] }), pero el
 * Respond to Webhook de n8n puede, segun su configuracion, colapsar ese
 * array de un item a un objeto suelto ({@code { "success":..., "data":... } }) —
 * ver javadoc de {@link #deserializarRespuesta}. En vez de exigirle a n8n que
 * garantice siempre la forma array (algo que este backend no controla),
 * {@code N8nClient} detecta la raiz real del JSON recibido y normaliza ambas
 * formas a {@code List<N8nAnalisisResponseDTO>} ANTES de devolver el
 * resultado. El resto del backend ({@code N8nService}, {@code LicitacionService},
 * {@code ExcelService}) nunca se entera de cual de las dos formas mando n8n:
 * siempre recibe la lista ya normalizada, con el mismo contrato de siempre.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class N8nClient {

    private static final int MAX_CARACTERES_LOG_RESPUESTA = 500;

    private final RestTemplate restTemplate;
    private final N8nProperties n8nProperties;
    private final ObjectMapper objectMapper;

    /** Tipo de la respuesta cruda de n8n cuando su raiz es ARRAY: un array con un unico resultado. */
    private static final JavaType TIPO_RESPUESTA_N8N =
            new ObjectMapper().getTypeFactory().constructCollectionType(List.class, N8nAnalisisResponseDTO.class);

    /**
     * Envia el identificador del proceso y los documentos del proceso
     * contractual al webhook de analisis de n8n, y espera de vuelta el
     * JSON estructurado con el resultado del analisis.
     *
     * n8n responde normalmente con un array de un solo elemento
     * ({@code [ { "success": true, "data": {...} } ] }); por eso el
     * contrato interno de este cliente sigue siendo
     * {@code List<N8nAnalisisResponseDTO>} y se toma el primer elemento. Si
     * n8n devuelve el objeto suelto en vez del array (ver
     * {@link #deserializarRespuesta}), se normaliza igual a una lista de un
     * elemento — el resto del backend (N8nService, LicitacionService,
     * ExcelService) sigue trabajando siempre con un
     * {@link N8nAnalisisResponseDTO} normal, sin enterarse de cual de las
     * dos formas mando n8n.
     *
     * @throws N8nCommunicationException si la peticion falla, la respuesta
     *                                    no puede procesarse, o el array
     *                                    viene vacio.
     */
    public N8nAnalisisResponseDTO enviarDocumentosParaAnalisis(Long procesoId, List<MultipartFile> documentos) {
        String url = n8nProperties.baseUrl() + n8nProperties.webhookAnalizar();

        MultiValueMap<String, Object> body = construirCuerpoMultipart(procesoId, documentos);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        log.info("Enviando {} documento(s) del proceso {} a n8n ({})",
                documentos == null ? 0 : documentos.size(), procesoId, url);

        ResponseEntity<String> response = ejecutarLlamadaHttp(procesoId, url, request);

        String cuerpoCrudo = response.getBody();
        log.info("Respuesta HTTP de n8n para el proceso {}: status={}, contentType={}, tamano={} caracter(es)",
                procesoId, response.getStatusCode(), response.getHeaders().getContentType(),
                cuerpoCrudo == null ? 0 : cuerpoCrudo.length());
        log.debug("Primeros {} caracteres de la respuesta de n8n para el proceso {}: {}",
                MAX_CARACTERES_LOG_RESPUESTA, procesoId, resumen(cuerpoCrudo));

        if (cuerpoCrudo == null || cuerpoCrudo.isBlank()) {
            throw new N8nCommunicationException("n8n respondio sin cuerpo (HTTP " + response.getStatusCode()
                    + ") para el proceso " + procesoId);
        }

        List<N8nAnalisisResponseDTO> resultados = deserializarRespuesta(procesoId, cuerpoCrudo);

        if (resultados.isEmpty()) {
            throw new N8nCommunicationException("n8n respondio sin contenido para el proceso " + procesoId);
        }
        N8nAnalisisResponseDTO resultado = resultados.get(0);

        if (!Boolean.TRUE.equals(resultado.getSuccess()) || resultado.getData() == null) {
            // Fallar rapido en vez de propagar un resultado con
            // fichaTecnica=null que el frontend interpretaria como
            // "el analisis no devolvio ficha tecnica" sin explicacion.
            throw new N8nCommunicationException(
                    "n8n reporto un analisis sin exito (success=false o sin data) para el proceso " + procesoId);
        }
        return resultado;
    }

    /**
     * Paso 1: solo transporte. Si esto falla, el problema es de red, DNS,
     * timeout o un status HTTP de error de n8n — nunca de deserializacion,
     * porque aqui todavia no se interpreta el cuerpo como JSON tipado (se
     * pide como {@code String} tal cual).
     */
    private ResponseEntity<String> ejecutarLlamadaHttp(Long procesoId, String url,
                                                          HttpEntity<MultiValueMap<String, Object>> request) {
        try {
            return restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        } catch (RestClientException ex) {
            Throwable causaRaiz = NestedExceptionUtils.getMostSpecificCause(ex);
            log.error("Fallo de TRANSPORTE comunicandose con n8n para el proceso {} ({}): {}",
                    procesoId, url, causaRaiz.getMessage(), ex);
            throw new N8nCommunicationException(
                    "Fallo la comunicacion con n8n para el proceso " + procesoId + ": " + causaRaiz.getMessage(), ex);
        }
    }

    /**
     * Paso 2: formato + normalizacion ARRAY/OBJECT. n8n ya respondio (2xx,
     * cuerpo capturado y logueado arriba). Primero se parsea el cuerpo a un
     * {@link JsonNode} (sin tipar todavia) para poder inspeccionar el tipo
     * de la raiz ANTES de pedirle a Jackson que la mapee directamente contra
     * {@code List<N8nAnalisisResponseDTO>} — eso es lo que evita el error
     * generico "Cannot deserialize ... ArrayList ... from Object value
     * (token START_OBJECT)": ese error solo ocurre cuando se le pide a
     * Jackson deserializar DIRECTAMENTE una raiz OBJECT como si fuera una
     * List; aqui nunca se hace esa llamada directa, siempre se inspecciona
     * el {@link JsonNode} primero y se elige la conversion correcta segun lo
     * que realmente haya en la raiz:
     *
     * <ul>
     *   <li>raiz ARRAY -&gt; se convierte a {@code List<N8nAnalisisResponseDTO>}
     *       tal cual (comportamiento de siempre).</li>
     *   <li>raiz OBJECT -&gt; se convierte a un {@code N8nAnalisisResponseDTO}
     *       unico y se envuelve en {@link Collections#singletonList}, para
     *       que el resto del backend nunca note la diferencia.</li>
     *   <li>raiz null/string/numero/boolean -&gt; ni ARRAY ni OBJECT son
     *       formas validas del contrato; se rechaza explicitamente en vez de
     *       dejar que Jackson tire un error generico.</li>
     * </ul>
     *
     * Cualquier fallo de conversion (contenido de "data"/"fichaTecnica"/etc.
     * que no coincide con los DTO, para cualquiera de las dos raices) sigue
     * reportando la causa raiz de Jackson completa, igual que antes.
     */
    private List<N8nAnalisisResponseDTO> deserializarRespuesta(Long procesoId, String cuerpoCrudo) {
        JsonNode raiz = parsearJson(procesoId, cuerpoCrudo);
        String tipoRaiz = tipoDeRaiz(raiz);

        if (raiz.isArray()) {
            log.info("Respuesta n8n recibida: proceso={}, status=200, root={}", procesoId, tipoRaiz);
            return convertir(procesoId, cuerpoCrudo, tipoRaiz, () -> objectMapper.convertValue(raiz, TIPO_RESPUESTA_N8N));
        }

        if (raiz.isObject()) {
            log.info("Respuesta n8n recibida: proceso={}, status=200, root={}", procesoId, tipoRaiz);
            log.info("Normalizando OBJECT -> List de un elemento para el proceso {}", procesoId);
            N8nAnalisisResponseDTO unico = convertir(procesoId, cuerpoCrudo, tipoRaiz,
                    () -> objectMapper.convertValue(raiz, N8nAnalisisResponseDTO.class));
            return Collections.singletonList(unico);
        }

        // Ni ARRAY ni OBJECT: null, string, numero o boolean sueltos en la raiz.
        // No es un problema de transporte (n8n respondio 200) ni de un campo
        // interno puntual: es que la raiz misma no tiene una forma que el
        // contrato pueda representar.
        String mensaje = "Respuesta de n8n con raiz JSON invalida (" + tipoRaiz + "). Se esperaba ARRAY u OBJECT, "
                + "para el proceso " + procesoId;
        log.error("{}. Primeros {} caracteres del cuerpo: {}",
                mensaje, MAX_CARACTERES_LOG_RESPUESTA, resumen(cuerpoCrudo));
        throw new N8nCommunicationException(mensaje);
    }

    /** Parsea el cuerpo crudo a {@link JsonNode}; si ni siquiera es JSON valido, falla como error de contrato. */
    private JsonNode parsearJson(Long procesoId, String cuerpoCrudo) {
        try {
            return objectMapper.readTree(cuerpoCrudo);
        } catch (JsonProcessingException ex) {
            Throwable causaRaiz = NestedExceptionUtils.getMostSpecificCause(ex);
            log.error("La respuesta de n8n para el proceso {} no es JSON valido. Causa: {}. Primeros {} caracteres: {}",
                    procesoId, causaRaiz.getMessage(), MAX_CARACTERES_LOG_RESPUESTA, resumen(cuerpoCrudo), ex);
            throw new N8nCommunicationException(
                    "n8n respondio con un cuerpo que no es JSON valido para el proceso " + procesoId + ": "
                            + causaRaiz.getMessage(), ex);
        }
    }

    /** Ejecuta la conversion Jackson (ARRAY u OBJECT) y homogeniza el manejo de errores de contrato. */
    private <T> T convertir(Long procesoId, String cuerpoCrudo, String tipoRaiz, java.util.function.Supplier<T> conversion) {
        try {
            return conversion.get();
        } catch (IllegalArgumentException ex) {
            // ObjectMapper#convertValue envuelve cualquier JsonMappingException
            // interna en IllegalArgumentException; la causa raiz de Jackson
            // (campo/reference chain exacto) se recupera igual con
            // NestedExceptionUtils, como ya se hacia antes de esta normalizacion.
            Throwable causaRaiz = NestedExceptionUtils.getMostSpecificCause(ex);
            log.error("La respuesta de n8n para el proceso {} tiene raiz {} pero su contenido no coincide con el "
                            + "contrato esperado. Causa Jackson: {}. Primeros {} caracteres del cuerpo: {}",
                    procesoId, tipoRaiz, causaRaiz.getMessage(), MAX_CARACTERES_LOG_RESPUESTA, resumen(cuerpoCrudo), ex);
            throw new N8nCommunicationException(
                    "n8n respondio con un JSON que no coincide con el contrato esperado para el proceso "
                            + procesoId + ": " + causaRaiz.getMessage(), ex);
        }
    }

    /** Nombre legible del tipo de nodo raiz, para logging y mensajes de error. */
    private String tipoDeRaiz(JsonNode nodo) {
        if (nodo == null || nodo.isMissingNode()) {
            return "MISSING";
        }
        if (nodo.isArray()) {
            return "ARRAY";
        }
        if (nodo.isObject()) {
            return "OBJECT";
        }
        if (nodo.isNull()) {
            return "NULL";
        }
        if (nodo.isTextual()) {
            return "STRING";
        }
        if (nodo.isNumber()) {
            return "NUMBER";
        }
        if (nodo.isBoolean()) {
            return "BOOLEAN";
        }
        return "UNKNOWN";
    }

    /** Trunca el cuerpo para el log: evita volcar documentos/respuestas completas potencialmente enormes. */
    private String resumen(String cuerpo) {
        if (cuerpo == null) {
            return "(sin cuerpo)";
        }
        return cuerpo.length() <= MAX_CARACTERES_LOG_RESPUESTA
                ? cuerpo
                : cuerpo.substring(0, MAX_CARACTERES_LOG_RESPUESTA) + "... (truncado, " + cuerpo.length() + " caracteres en total)";
    }

    private MultiValueMap<String, Object> construirCuerpoMultipart(Long procesoId, List<MultipartFile> documentos) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("procesoId", procesoId);

        if (documentos != null) {
            for (MultipartFile documento : documentos) {
                body.add("documentos", convertirAResource(documento));
            }
        }
        return body;
    }

    private ByteArrayResource convertirAResource(MultipartFile documento) {
        try {
            return new ByteArrayResource(documento.getBytes()) {
                @Override
                public String getFilename() {
                    return documento.getOriginalFilename();
                }
            };
        } catch (IOException ex) {
            throw new N8nCommunicationException(
                    "No se pudo leer el archivo '" + documento.getOriginalFilename() + "' para enviarlo a n8n", ex);
        }
    }
}
