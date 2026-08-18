package com.licitaciones.backend.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.licitaciones.backend.config.N8nProperties;
import com.licitaciones.backend.dto.response.CargoDTO;
import com.licitaciones.backend.dto.response.DocumentacionItemDTO;
import com.licitaciones.backend.dto.response.N8nAnalisisResponseDTO;
import com.licitaciones.backend.dto.response.PerfilDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JSON REAL del proceso 21, probado a traves de {@link N8nClient} completo
 * (transporte HTTP simulado + normalizacion ARRAY/OBJECT + deserializacion),
 * en sus DOS variantes posibles de raiz:
 *
 * <ul>
 *   <li>VARIANTE A: el objeto envuelto en un array de un elemento
 *       ({@code [ {...} ] }) — la forma "oficial" documentada.</li>
 *   <li>VARIANTE B: el mismo objeto SIN el array envolvente
 *       ({@code {...} }) — la forma que produce el Respond to Webhook de
 *       n8n cuando colapsa el array de un item a un objeto suelto.</li>
 * </ul>
 *
 * Ambas variantes deben producir, a traves de
 * {@link N8nClient#enviarDocumentosParaAnalisis}, el MISMO
 * {@link N8nAnalisisResponseDTO} con los mismos datos (fichaTecnica,
 * documentacion, perfiles, requisitosFinancieros, codigosRUP, pagina en sus
 * formas Integer y arreglo) — el resto del backend nunca deberia poder
 * distinguir cual de las dos raices envio n8n.
 */
class N8nClientProceso21NormalizacionTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer servidorSimulado;
    private N8nClient n8nClient;

    private static final String URL_WEBHOOK = "http://n8n.local/webhook/analizar";

    /** El objeto "{ success, data }" del proceso 21, SIN el array envolvente (variante B, tal cual). */
    private static final String OBJETO_PROCESO_21 = """
            {
                "success": true,
                "data": {
                  "fichaTecnica": {
                    "archivo": "FO-GECO-016ProyectoPliegodeCondicionesLPDiferentedeObraSECOPII_v4_70007713.docx.pdf",
                    "nombreProyecto": { "valor": "70007713", "pagina": 1 },
                    "entidad": { "valor": "Distrito Especial de Ciencia, Tecnología e Innovación de Medellín", "pagina": 3 },
                    "objetoContrato": { "valor": "Prestar el servicio de soporte, mantenimiento, evolución y desarrollo de los sistemas de información del Distrito.", "pagina": 4 },
                    "presupuesto": { "valor": 2424588580, "pagina": 5 },
                    "plazoProyecto": { "valor": "SEIS MESES (6), sin superar la presente vigencia 2025", "pagina": 5 },
                    "ciudad": { "valor": "Medellín", "pagina": 1 },
                    "valoracion": { "valor": "Precio (600 puntos)", "pagina": 41 },
                    "codigosRUP": { "valor": [81111500, 81111600, 81111900, 81112200], "pagina": [6, 25] },
                    "requisitosFinancieros": { "IndiceLiquidez": 1.3, "NivelEndeudamiento": 0.7, "CoberturaIntereses": 1, "ROE": 0.04, "ROA": 0.02, "pagina": 35 }
                  },
                  "documentacion": {
                    "archivo": "Anexo 1. Estudio Previo 39118.pdf",
                    "capacidadJuridica": [
                      { "id": 1, "documento": "Acreditación de Emprendimientos y Empresas de Mujeres", "pagina": 8 }
                    ],
                    "capacidadTecnica": [
                      { "id": 3, "documento": "Certificaciones de experiencia", "pagina": [12, 13] }
                    ],
                    "criteriosDeEvaluacion": [
                      { "id": 1, "documento": "Precio (600 puntos)", "pagina": [17, 19] }
                    ]
                  },
                  "perfiles": [
                    {
                      "archivo": "Anexo 1. Estudio Previo 39118.pdf",
                      "pagina": 16,
                      "cargos": [
                        { "cargo": "Desarrolladores FullStack", "cantidad": 16, "perfil": "Ingeniero de sistemas..." }
                      ]
                    }
                  ],
                  "polizas": null
                }
              }
            """;

    /** VARIANTE A: el mismo objeto, envuelto en el array de un elemento. */
    private static final String ARRAY_PROCESO_21 = "[\n" + OBJETO_PROCESO_21 + "\n]";

    @BeforeEach
    void configurar() {
        restTemplate = new RestTemplate();
        servidorSimulado = MockRestServiceServer.createServer(restTemplate);
        N8nProperties propiedades = new N8nProperties("http://n8n.local", "/webhook/analizar", 5000, 5000);
        n8nClient = new N8nClient(restTemplate, propiedades, new ObjectMapper());
    }

    @Test
    void varianteA_arrayDeUnElemento_funciona() {
        servidorSimulado.expect(MockRestRequestMatchers.requestTo(URL_WEBHOOK))
                .andRespond(MockRestResponseCreators.withSuccess(ARRAY_PROCESO_21, MediaType.APPLICATION_JSON));

        N8nAnalisisResponseDTO resultado = n8nClient.enviarDocumentosParaAnalisis(21L, java.util.List.of());

        verificarDatosCompletos(resultado);
    }

    @Test
    void varianteB_objetoSuelto_seNormalizaYFuncionaIgual() {
        servidorSimulado.expect(MockRestRequestMatchers.requestTo(URL_WEBHOOK))
                .andRespond(MockRestResponseCreators.withSuccess(OBJETO_PROCESO_21, MediaType.APPLICATION_JSON));

        N8nAnalisisResponseDTO resultado = n8nClient.enviarDocumentosParaAnalisis(21L, java.util.List.of());

        verificarDatosCompletos(resultado);
    }

    /** Mismas aserciones para ambas variantes: A y B deben producir exactamente el mismo resultado. */
    private void verificarDatosCompletos(N8nAnalisisResponseDTO resultado) {
        assertThat(resultado.getSuccess()).isTrue();
        assertThat(resultado.getData()).isNotNull();

        // fichaTecnica
        var ficha = resultado.getData().getFichaTecnica();
        assertThat(ficha).isNotNull();
        assertThat(ficha.getNombreProyecto().getValor()).isEqualTo("70007713");
        assertThat(ficha.getPresupuesto().getValor().longValue()).isEqualTo(2424588580L);
        assertThat(ficha.getPresupuesto().getPagina()).isEqualTo(5); // pagina Integer

        // codigosRUP: sigue siendo List<Long> / List<Integer>, no tocado por la normalizacion
        assertThat(ficha.getCodigosRUP().getValor()).containsExactly(81111500L, 81111600L, 81111900L, 81112200L);
        assertThat(ficha.getCodigosRUP().getPagina()).containsExactly(6, 25);

        // requisitosFinancieros
        assertThat(ficha.getRequisitosFinancieros().getIndiceLiquidez()).isEqualByComparingTo("1.3");
        assertThat(ficha.getRequisitosFinancieros().getPagina()).isEqualTo(35);

        // documentacion, con pagina como arreglo
        var documentacion = resultado.getData().getDocumentacion();
        assertThat(documentacion).isNotNull();
        DocumentacionItemDTO itemCapacidadTecnica = documentacion.getCapacidadTecnica().get(0);
        assertThat(itemCapacidadTecnica.getPagina()).containsExactly(12, 13); // [12,13] -> se conservan ambas
        assertThat(documentacion.getCriteriosDeEvaluacion().get(0).getPagina()).containsExactly(17, 19); // [17,19] -> se conservan ambas

        // perfiles
        PerfilDTO grupo = resultado.getData().getPerfiles().get(0);
        assertThat(grupo.getPagina()).isEqualTo(16);
        CargoDTO cargo = grupo.getCargos().get(0);
        assertThat(cargo.getCargo()).isEqualTo("Desarrolladores FullStack");
        assertThat(cargo.getCantidad()).isEqualTo(16);
    }
}
