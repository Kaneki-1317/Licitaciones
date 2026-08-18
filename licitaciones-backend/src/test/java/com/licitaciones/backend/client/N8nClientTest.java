package com.licitaciones.backend.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.licitaciones.backend.config.N8nProperties;
import com.licitaciones.backend.dto.response.N8nAnalisisResponseDTO;
import com.licitaciones.backend.exception.N8nCommunicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba de {@link N8nClient} contra una respuesta HTTP simulada con la
 * MISMA forma que envia n8n hoy (array raiz de un solo elemento), usando
 * {@link MockRestServiceServer} en vez de tocar la red real. Cubre
 * exactamente el escenario que causaba el 502 antes de este arreglo.
 */
class N8nClientTest {

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
    void desempacaElArrayRaizYDevuelveElPrimerResultado() {
        String jsonArrayDeUnElemento = """
                [
                  {
                    "success": true,
                    "data": {
                      "fichaTecnica": {
                        "archivo": "pliego.pdf",
                        "entidad": { "valor": "Alcaldia de Ejemplo", "pagina": 3 }
                      },
                      "documentacion": { "archivo": "anexo.pdf", "capacidadJuridica": [], "capacidadTecnica": [], "criteriosDeEvaluacion": [] },
                      "perfiles": [],
                      "polizas": null
                    }
                  }
                ]
                """;

        servidorSimulado.expect(MockRestRequestMatchers.requestTo(URL_WEBHOOK))
                .andRespond(MockRestResponseCreators.withSuccess(jsonArrayDeUnElemento, MediaType.APPLICATION_JSON));

        N8nAnalisisResponseDTO resultado = n8nClient.enviarDocumentosParaAnalisis(20L, List.of());

        servidorSimulado.verify();
        assertThat(resultado.getSuccess()).isTrue();
        assertThat(resultado.getData().getFichaTecnica().getEntidad().getValor()).isEqualTo("Alcaldia de Ejemplo");
    }

    @Test
    void desempacaElPayloadRealDelProceso21ConPaginasEnArreglo() {
        // Payload real reportado en el ticket del proceso 21: capacidadTecnica
        // id 3 trae "pagina": [12, 13] y criteriosDeEvaluacion id 1/2 traen
        // "pagina": [17, 19]. Antes del PaginaFlexibleDeserializer, esto
        // rompia todo el List<N8nAnalisisResponseDTO> con
        // "Error while extracting response for type [...]" (502).
        String jsonProceso21 = """
                [
                  {
                    "success": true,
                    "data": {
                      "fichaTecnica": {
                        "archivo": "pliego.pdf",
                        "presupuesto": { "valor": 2424588580, "pagina": 5 }
                      },
                      "documentacion": {
                        "archivo": "anexo.pdf",
                        "capacidadJuridica": [],
                        "capacidadTecnica": [
                          { "id": 3, "documento": "Certificaciones de experiencia", "pagina": [12, 13] }
                        ],
                        "criteriosDeEvaluacion": [
                          { "id": 1, "documento": "Precio (600 puntos)", "pagina": [17, 19] }
                        ]
                      },
                      "perfiles": [],
                      "polizas": null
                    }
                  }
                ]
                """;

        servidorSimulado.expect(MockRestRequestMatchers.requestTo(URL_WEBHOOK))
                .andRespond(MockRestResponseCreators.withSuccess(jsonProceso21, MediaType.APPLICATION_JSON));

        N8nAnalisisResponseDTO resultado = n8nClient.enviarDocumentosParaAnalisis(21L, List.of());

        servidorSimulado.verify();
        assertThat(resultado.getData().getFichaTecnica().getPresupuesto().getValor().longValue())
                .isEqualTo(2424588580L);
        assertThat(resultado.getData().getDocumentacion().getCapacidadTecnica().get(0).getPagina()).containsExactly(12, 13);
        assertThat(resultado.getData().getDocumentacion().getCriteriosDeEvaluacion().get(0).getPagina()).containsExactly(17, 19);
    }

    @Test
    void arrayVacioLanzaN8nCommunicationException() {
        servidorSimulado.expect(MockRestRequestMatchers.requestTo(URL_WEBHOOK))
                .andRespond(MockRestResponseCreators.withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> n8nClient.enviarDocumentosParaAnalisis(20L, List.of()))
                .isInstanceOf(N8nCommunicationException.class)
                .hasMessageContaining("sin contenido");
    }

    @Test
    void successFalseLanzaN8nCommunicationException() {
        String jsonSinExito = """
                [ { "success": false, "data": null } ]
                """;

        servidorSimulado.expect(MockRestRequestMatchers.requestTo(URL_WEBHOOK))
                .andRespond(MockRestResponseCreators.withSuccess(jsonSinExito, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> n8nClient.enviarDocumentosParaAnalisis(20L, List.of()))
                .isInstanceOf(N8nCommunicationException.class)
                .hasMessageContaining("sin exito");
    }

    @Test
    void arrayN8nSeDeserializaCorrectamente() {
        // FORMATO A (contrato "oficial"): array de un elemento.
        String jsonArray = """
                [
                  {
                    "success": true,
                    "data": {
                      "fichaTecnica": {
                        "archivo": "test.pdf",
                        "nombreProyecto": { "valor": "123", "pagina": 1 }
                      }
                    }
                  }
                ]
                """;

        servidorSimulado.expect(MockRestRequestMatchers.requestTo(URL_WEBHOOK))
                .andRespond(MockRestResponseCreators.withSuccess(jsonArray, MediaType.APPLICATION_JSON));

        N8nAnalisisResponseDTO resultado = n8nClient.enviarDocumentosParaAnalisis(20L, List.of());

        assertThat(resultado.getSuccess()).isTrue();
        assertThat(resultado.getData().getFichaTecnica().getNombreProyecto().getValor()).isEqualTo("123");
        assertThat(resultado.getData().getFichaTecnica().getNombreProyecto().getPagina()).isEqualTo(1);
    }

    @Test
    void objectN8nSeNormalizaAListaCorrectamente() {
        // FORMATO B (obligatorio soportar, sin exigirle a n8n que cambie
        // nada): el mismo contenido de "success"/"data", SIN el array
        // envolvente. N8nClient debe detectar la raiz OBJECT y normalizarla
        // a una lista de un elemento -antes, esto lanzaba
        // N8nCommunicationException con "raiz OBJECT... ARRAY"; ahora debe
        // funcionar igual que el FORMATO A.
        String jsonObject = """
                {
                  "success": true,
                  "data": {
                    "fichaTecnica": {
                      "archivo": "test.pdf",
                      "nombreProyecto": { "valor": "123", "pagina": 1 }
                    }
                  }
                }
                """;

        servidorSimulado.expect(MockRestRequestMatchers.requestTo(URL_WEBHOOK))
                .andRespond(MockRestResponseCreators.withSuccess(jsonObject, MediaType.APPLICATION_JSON));

        N8nAnalisisResponseDTO resultado = n8nClient.enviarDocumentosParaAnalisis(20L, List.of());

        assertThat(resultado.getSuccess()).isTrue();
        assertThat(resultado.getData().getFichaTecnica().getNombreProyecto().getValor()).isEqualTo("123");
        assertThat(resultado.getData().getFichaTecnica().getNombreProyecto().getPagina()).isEqualTo(1);
    }
}
