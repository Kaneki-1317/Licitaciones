package com.licitaciones.backend.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.licitaciones.backend.config.N8nProperties;
import com.licitaciones.backend.dto.response.N8nAnalisisResponseDTO;
import com.licitaciones.backend.exception.N8nCommunicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AUDITORIA FORENSE — matriz HTTP completa pedida explicitamente: no alcanza
 * con probar {@code ObjectMapper.readValue(...)} suelto (eso ya lo cubren
 * {@code N8nAnalisisResponse*Test}); hay que recorrer el mismo camino que
 * recorre una peticion real -- {@link N8nClient#enviarDocumentosParaAnalisis}
 * completo, incluyendo el paso de transporte HTTP -- con {@link MockRestServiceServer}
 * simulando cada respuesta posible de n8n.
 *
 * Cubre los 9 escenarios pedidos:
 *   1) HTTP 200 + raiz ARRAY            -> exito
 *   2) HTTP 200 + raiz OBJECT           -> exito (normalizado a lista de 1 elemento)
 *   3) HTTP 404                         -> falla de TRANSPORTE (no de deserializacion)
 *   4) HTTP 502 (n8n devuelve 502)      -> falla de TRANSPORTE (no de deserializacion)
 *   5) HTTP 200 + pagina Integer        -> exito
 *   6) HTTP 200 + pagina Array (documentacion) -> exito (conserva TODAS las paginas)
 *   7) HTTP 200 + pagina null           -> exito (pagina queda null)
 *   8) HTTP 200 + JSON invalido         -> falla de CONTRATO (JSON malformado)
 *   9) HTTP 200 + raiz null             -> falla de CONTRATO ("raiz JSON invalida (NULL)")
 */
class N8nClientMatrizHttpTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer servidorSimulado;
    private N8nClient n8nClient;

    private static final String URL_WEBHOOK = "http://n8n.local/webhook/analizar";

    @BeforeEach
    void configurar() {
        restTemplate = new RestTemplate();
        servidorSimulado = MockRestServiceServer.createServer(restTemplate);
        N8nProperties propiedades = new N8nProperties("http://n8n.local", "/webhook/analizar", 5000, 5000);
        n8nClient = new N8nClient(restTemplate, propiedades, new ObjectMapper());
    }

    @Test
    void escenario1_http200RaizArray_exito() {
        String json = """
                [ { "success": true, "data": { "fichaTecnica": { "archivo": "x.pdf" } } } ]
                """;
        servidorSimulado.expect(MockRestRequestMatchers.requestTo(URL_WEBHOOK))
                .andRespond(MockRestResponseCreators.withSuccess(json, MediaType.APPLICATION_JSON));

        N8nAnalisisResponseDTO resultado = n8nClient.enviarDocumentosParaAnalisis(1L, List.of());

        assertThat(resultado.getSuccess()).isTrue();
    }

    @Test
    void escenario2_http200RaizObject_exitoNormalizadoAListaDeUnElemento() {
        // n8n devuelve el objeto suelto (sin el array envolvente); N8nClient
        // debe detectar la raiz OBJECT y normalizarla a una lista de 1
        // elemento -- el resultado debe ser IDENTICO al de una raiz ARRAY
        // equivalente (escenario1), sin lanzar ninguna excepcion.
        String json = """
                { "success": true, "data": { "fichaTecnica": { "archivo": "x.pdf" } } }
                """;
        servidorSimulado.expect(MockRestRequestMatchers.requestTo(URL_WEBHOOK))
                .andRespond(MockRestResponseCreators.withSuccess(json, MediaType.APPLICATION_JSON));

        N8nAnalisisResponseDTO resultado = n8nClient.enviarDocumentosParaAnalisis(2L, List.of());

        assertThat(resultado.getSuccess()).isTrue();
        assertThat(resultado.getData().getFichaTecnica().getArchivo()).isEqualTo("x.pdf");
    }

    @Test
    void escenario3_http404_fallaDeTransporteNoDeFormato() {
        servidorSimulado.expect(MockRestRequestMatchers.requestTo(URL_WEBHOOK))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":404,\"message\":\"not registered\"}"));

        // Debe fallar en el paso de TRANSPORTE (mensaje sobre "comunicacion",
        // trae el 404 crudo), nunca llega a intentar deserializar como
        // List<N8nAnalisisResponseDTO> -> nunca puede decir "raiz OBJECT" ni
        // "no coincide con el contrato".
        assertThatThrownBy(() -> n8nClient.enviarDocumentosParaAnalisis(3L, List.of()))
                .isInstanceOf(N8nCommunicationException.class)
                .hasMessageContaining("Fallo la comunicacion con n8n")
                .hasMessageContaining("404")
                .hasMessageNotContaining("raiz OBJECT")
                .hasMessageNotContaining("no coincide con el contrato");
    }

    @Test
    void escenario4_http502DeN8n_fallaDeTransporteNoDeFormato() {
        servidorSimulado.expect(MockRestRequestMatchers.requestTo(URL_WEBHOOK))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.BAD_GATEWAY)
                        .contentType(MediaType.TEXT_HTML)
                        .body("<html>502 Bad Gateway</html>"));

        assertThatThrownBy(() -> n8nClient.enviarDocumentosParaAnalisis(4L, List.of()))
                .isInstanceOf(N8nCommunicationException.class)
                .hasMessageContaining("Fallo la comunicacion con n8n")
                .hasMessageContaining("502");
    }

    @Test
    void escenario5_http200ConPaginaEntero_exito() {
        String json = """
                [ { "success": true, "data": { "fichaTecnica": {
                      "archivo": "x.pdf",
                      "nombreProyecto": { "valor": "70007713", "pagina": 1 }
                    } } } ]
                """;
        servidorSimulado.expect(MockRestRequestMatchers.requestTo(URL_WEBHOOK))
                .andRespond(MockRestResponseCreators.withSuccess(json, MediaType.APPLICATION_JSON));

        N8nAnalisisResponseDTO resultado = n8nClient.enviarDocumentosParaAnalisis(5L, List.of());

        assertThat(resultado.getData().getFichaTecnica().getNombreProyecto().getPagina()).isEqualTo(1);
    }

    @Test
    void escenario6_http200ConPaginaArreglo_exitoConservaTodasLasPaginas() {
        String json = """
                [ { "success": true, "data": { "documentacion": {
                      "archivo": "anexo.pdf",
                      "capacidadJuridica": [ { "id": 1, "documento": "RUP", "pagina": [7,12,13,15] } ],
                      "capacidadTecnica": [], "criteriosDeEvaluacion": []
                    } } } ]
                """;
        servidorSimulado.expect(MockRestRequestMatchers.requestTo(URL_WEBHOOK))
                .andRespond(MockRestResponseCreators.withSuccess(json, MediaType.APPLICATION_JSON));

        N8nAnalisisResponseDTO resultado = n8nClient.enviarDocumentosParaAnalisis(6L, List.of());

        assertThat(resultado.getData().getDocumentacion().getCapacidadJuridica().get(0).getPagina())
                .containsExactly(7, 12, 13, 15);
    }

    @Test
    void escenario7_http200ConPaginaNull_exitoQuedaNull() {
        String json = """
                [ { "success": true, "data": { "fichaTecnica": {
                      "archivo": "x.pdf",
                      "nombreProyecto": { "valor": "70007713", "pagina": null }
                    } } } ]
                """;
        servidorSimulado.expect(MockRestRequestMatchers.requestTo(URL_WEBHOOK))
                .andRespond(MockRestResponseCreators.withSuccess(json, MediaType.APPLICATION_JSON));

        N8nAnalisisResponseDTO resultado = n8nClient.enviarDocumentosParaAnalisis(7L, List.of());

        assertThat(resultado.getData().getFichaTecnica().getNombreProyecto().getPagina()).isNull();
    }

    @Test
    void escenario8_http200ConJsonInvalido_fallaDeContrato() {
        // No es transporte (HTTP 200) ni un problema de raiz ARRAY/OBJECT:
        // el cuerpo directamente no es JSON valido (llave sin cerrar).
        String jsonRoto = "{ \"success\": true, \"data\": ";
        servidorSimulado.expect(MockRestRequestMatchers.requestTo(URL_WEBHOOK))
                .andRespond(MockRestResponseCreators.withSuccess(jsonRoto, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> n8nClient.enviarDocumentosParaAnalisis(8L, List.of()))
                .isInstanceOf(N8nCommunicationException.class)
                .hasMessageContaining("no es JSON valido");
    }

    @Test
    void escenario9_http200ConRaizNull_fallaDeContrato() {
        // Cuerpo JSON valido, pero su raiz es el literal "null" -- ni ARRAY
        // ni OBJECT. Debe rechazarse explicitamente en vez de que Jackson
        // intente convertir null a List y produzca un resultado ambiguo.
        servidorSimulado.expect(MockRestRequestMatchers.requestTo(URL_WEBHOOK))
                .andRespond(MockRestResponseCreators.withSuccess("null", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> n8nClient.enviarDocumentosParaAnalisis(9L, List.of()))
                .isInstanceOf(N8nCommunicationException.class)
                .hasMessageContaining("raiz JSON invalida")
                .hasMessageContaining("NULL");
    }
}
