package com.licitaciones.backend.dto.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de deserializacion contra la respuesta EXACTA que envia n8n hoy
 * (array raiz de un elemento, fichaTecnica con campos {valor, pagina}, etc.)
 * usando el mismo ObjectMapper por defecto que usa RestTemplate (sin
 * configuracion extra: no hay beans de Jackson custom en el proyecto).
 *
 * Nota sobre el nombre de archivo: el JSON de referencia que origino este
 * arreglo traia una secuencia "\_" (backslash + guion bajo) en
 * fichaTecnica.archivo, que no es un escape valido en JSON estricto. Se usa
 * aqui el nombre sin esa secuencia porque el resto de nombres de archivo del
 * mismo payload (ej. "Anexo 1. Estudio Previo 39118.pdf") no la tienen, lo
 * que sugiere que fue un artefacto de como se pego el JSON en el chat y no
 * algo que n8n realmente envia. Si se confirma que n8n si envia backslashes
 * sueltos, ese es un problema de parseo JSON aparte (a nivel de tokenizer,
 * antes de llegar a este mapeo) y necesitaria su propio arreglo.
 */
class N8nAnalisisResponseDeserializationTest {

    private static final String JSON_RESPUESTA_N8N = """
            [
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
                    "valoracion": { "valor": "Precio: 600 puntos, Criterios de Calidad: 286 puntos. Total: 1.000 puntos", "pagina": 41 },
                    "codigosRUP": { "valor": [81111500, 81111600, 81111900, 81112200], "pagina": [6, 25] },
                    "requisitosFinancieros": { "IndiceLiquidez": 1.3, "NivelEndeudamiento": 0.7, "CoberturaIntereses": 1, "ROE": 0.04, "ROA": 0.02, "pagina": 35 }
                  },
                  "documentacion": {
                    "archivo": "Anexo 1. Estudio Previo 39118.pdf",
                    "capacidadJuridica": [
                      { "id": 1, "documento": "Formato Acreditación Emprendimiento y Empresa de Mujeres", "pagina": 8 },
                      { "id": 2, "documento": "Formato Acreditación de Mipyme", "pagina": 10 }
                    ],
                    "capacidadTecnica": [
                      { "id": 1, "documento": "Registro Único de Proponentes (RUP)", "pagina": 7 }
                    ],
                    "criteriosDeEvaluacion": [
                      { "id": 1, "documento": "Precio", "pagina": 17 },
                      { "id": 2, "documento": "Calidad", "pagina": 17 }
                    ]
                  },
                  "perfiles": [
                    {
                      "archivo": "Anexo 1. Estudio Previo 39118.pdf",
                      "pagina": 16,
                      "cargos": [
                        { "cargo": "Desarrolladores FullStack", "cantidad": 16, "perfil": "Ingeniero de sistemas..." },
                        { "cargo": "Líder User Experiencia - UX", "cantidad": 2, "perfil": "Profesional universitario..." }
                      ]
                    }
                  ],
                  "polizas": {
                    "nombreArchivo": "FO-GECO-016ProyectoPliegodeCondicionesLPDiferentedeObraSECOPII_v4_70007713.docx.pdf",
                    "paginasConsultadas": [42, 43],
                    "seriedad": { "valor": 10, "archivo": "FO-GECO-016ProyectoPliegodeCondicionesLPDiferentedeObraSECOPII_v4_70007713.docx.pdf", "pagina": 42 },
                    "cumplimiento": { "valor": 10, "archivo": "FO-GECO-016ProyectoPliegodeCondicionesLPDiferentedeObraSECOPII_v4_70007713.docx.pdf", "pagina": 42 },
                    "buenManejoAnticipo": { "valor": 0, "archivo": "FO-GECO-016ProyectoPliegodeCondicionesLPDiferentedeObraSECOPII_v4_70007713.docx.pdf", "pagina": 43 },
                    "pagoSalarios": { "valor": 5, "archivo": "FO-GECO-016ProyectoPliegodeCondicionesLPDiferentedeObraSECOPII_v4_70007713.docx.pdf", "pagina": 43 },
                    "estabilidad": { "valor": 0, "archivo": "FO-GECO-016ProyectoPliegodeCondicionesLPDiferentedeObraSECOPII_v4_70007713.docx.pdf", "pagina": 43 },
                    "calidad": { "valor": 10, "archivo": "FO-GECO-016ProyectoPliegodeCondicionesLPDiferentedeObraSECOPII_v4_70007713.docx.pdf", "pagina": 43 }
                  }
                }
              }
            ]
            """;

    @Test
    void deserializaElArrayRaizYDesempacaElPrimerElemento() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        List<N8nAnalisisResponseDTO> resultados =
                mapper.readValue(JSON_RESPUESTA_N8N, new TypeReference<List<N8nAnalisisResponseDTO>>() {
                });

        assertThat(resultados).hasSize(1);
        N8nAnalisisResponseDTO resultado = resultados.get(0);
        assertThat(resultado.getSuccess()).isTrue();
        assertThat(resultado.getData()).isNotNull();
    }

    @Test
    void deserializaFichaTecnicaConCamposTrazables() throws JsonProcessingException {
        FichaTecnicaDTO ficha = leerPrimerFichaTecnica();

        assertThat(ficha.getArchivo()).isEqualTo("FO-GECO-016ProyectoPliegodeCondicionesLPDiferentedeObraSECOPII_v4_70007713.docx.pdf");

        assertThat(ficha.getNombreProyecto().getValor()).isEqualTo("70007713");
        assertThat(ficha.getNombreProyecto().getPagina()).isEqualTo(1);

        assertThat(ficha.getEntidad().getValor()).isEqualTo("Distrito Especial de Ciencia, Tecnología e Innovación de Medellín");
        assertThat(ficha.getEntidad().getPagina()).isEqualTo(3);

        assertThat(ficha.getObjetoContrato().getValor())
                .isEqualTo("Prestar el servicio de soporte, mantenimiento, evolución y desarrollo de los sistemas de información del Distrito.");
        assertThat(ficha.getObjetoContrato().getPagina()).isEqualTo(4);

        assertThat(ficha.getPresupuesto().getValor()).isEqualByComparingTo(new BigDecimal("2424588580"));
        assertThat(ficha.getPresupuesto().getPagina()).isEqualTo(5);

        assertThat(ficha.getPlazoProyecto().getValor()).isEqualTo("SEIS MESES (6), sin superar la presente vigencia 2025");
        assertThat(ficha.getCiudad().getValor()).isEqualTo("Medellín");
        assertThat(ficha.getValoracion().getValor()).contains("Precio: 600 puntos");
    }

    @Test
    void deserializaCodigosRupConValorYPaginaDeTamanosDistintos() throws JsonProcessingException {
        FichaTecnicaDTO ficha = leerPrimerFichaTecnica();

        assertThat(ficha.getCodigosRUP().getValor()).containsExactly(81111500L, 81111600L, 81111900L, 81112200L);
        assertThat(ficha.getCodigosRUP().getPagina()).containsExactly(6, 25);
    }

    @Test
    void deserializaRequisitosFinancierosConAliasPascalCaseYPagina() throws JsonProcessingException {
        FichaTecnicaDTO ficha = leerPrimerFichaTecnica();
        RequisitosFinancierosDTO indicadores = ficha.getRequisitosFinancieros();

        assertThat(indicadores.getIndiceLiquidez()).isEqualByComparingTo(new BigDecimal("1.3"));
        assertThat(indicadores.getNivelEndeudamiento()).isEqualByComparingTo(new BigDecimal("0.7"));
        assertThat(indicadores.getCoberturaIntereses()).isEqualByComparingTo(new BigDecimal("1"));
        assertThat(indicadores.getRoe()).isEqualByComparingTo(new BigDecimal("0.04"));
        assertThat(indicadores.getRoa()).isEqualByComparingTo(new BigDecimal("0.02"));
        assertThat(indicadores.getPagina()).isEqualTo(35);
    }

    @Test
    void deserializaDocumentacionMapeandoArchivoANombreArchivoYConservaPaginaPorItem() throws JsonProcessingException {
        DocumentacionDTO documentacion = leerPrimerDocumentacion();

        assertThat(documentacion.getNombreArchivo()).isEqualTo("Anexo 1. Estudio Previo 39118.pdf");
        assertThat(documentacion.getCapacidadJuridica()).hasSize(2);
        assertThat(documentacion.getCapacidadJuridica().get(0).getId()).isEqualTo(1);
        assertThat(documentacion.getCapacidadJuridica().get(0).getDocumento())
                .isEqualTo("Formato Acreditación Emprendimiento y Empresa de Mujeres");
        assertThat(documentacion.getCapacidadJuridica().get(0).getPagina()).containsExactly(8);
        assertThat(documentacion.getCapacidadTecnica()).hasSize(1);
        assertThat(documentacion.getCriteriosDeEvaluacion()).hasSize(2);
    }

    @Test
    void deserializaPerfilesSinCambios() throws JsonProcessingException {
        List<PerfilDTO> perfiles = leerPrimerData().getPerfiles();

        assertThat(perfiles).hasSize(1);
        PerfilDTO grupo = perfiles.get(0);
        assertThat(grupo.getArchivo()).isEqualTo("Anexo 1. Estudio Previo 39118.pdf");
        assertThat(grupo.getPagina()).isEqualTo(16);
        assertThat(grupo.getCargos()).hasSize(2);
        assertThat(grupo.getCargos().get(0).getCargo()).isEqualTo("Desarrolladores FullStack");
        assertThat(grupo.getCargos().get(0).getCantidad()).isEqualTo(16);
        assertThat(grupo.getCargos().get(0).getPerfil()).isEqualTo("Ingeniero de sistemas...");
    }

    @Test
    void deserializaPolizasConValorArchivoYPagina() throws JsonProcessingException {
        PolizasDTO polizas = leerPrimerData().getPolizas();

        assertThat(polizas).isNotNull();
        assertThat(polizas.getNombreArchivo())
                .isEqualTo("FO-GECO-016ProyectoPliegodeCondicionesLPDiferentedeObraSECOPII_v4_70007713.docx.pdf");
        assertThat(polizas.getPaginasConsultadas()).containsExactly(42, 43);
        assertThat(polizas.getSeriedad().getValor()).isEqualTo(10);
        assertThat(polizas.getSeriedad().getArchivo())
                .isEqualTo("FO-GECO-016ProyectoPliegodeCondicionesLPDiferentedeObraSECOPII_v4_70007713.docx.pdf");
        assertThat(polizas.getSeriedad().getPagina()).isEqualTo(42);
        assertThat(polizas.getCumplimiento().getValor()).isEqualTo(10);
        // 0 es un valor valido (poliza no exigida), no debe perderse ni tratarse como ausente.
        assertThat(polizas.getBuenManejoAnticipo().getValor()).isZero();
        assertThat(polizas.getBuenManejoAnticipo().getPagina()).isEqualTo(43);
        assertThat(polizas.getPagoSalarios().getValor()).isEqualTo(5);
        assertThat(polizas.getEstabilidad().getValor()).isZero();
        assertThat(polizas.getCalidad().getValor()).isEqualTo(10);
    }

    @Test
    void polizasNuloNoRompeLaDeserializacion() throws JsonProcessingException {
        // Resultados guardados antes de que "polizas" existiera, o payload
        // con "polizas": null explicito: el campo debe quedar en null sin
        // lanzar JsonProcessingException.
        String jsonSinPolizas = """
                [
                  {
                    "success": true,
                    "data": {
                      "fichaTecnica": { "archivo": "test.pdf" },
                      "polizas": null
                    }
                  }
                ]
                """;

        ObjectMapper mapper = new ObjectMapper();
        List<N8nAnalisisResponseDTO> resultados =
                mapper.readValue(jsonSinPolizas, new TypeReference<List<N8nAnalisisResponseDTO>>() {
                });

        assertThat(resultados.get(0).getData()).isNotNull();
        assertThat(resultados.get(0).getData().getPolizas()).isNull();
    }

    @Test
    void polizasConContratoViejoDeNumerosPeladosSiguenFuncionando() throws JsonProcessingException {
        // Compatibilidad hacia atras: mientras n8n no envie archivo/pagina
        // POR POLIZA, "seriedad": 10 debe seguir deserializando sin error,
        // sin inventar archivo ni pagina individual. Este es exactamente el
        // JSON real que envia n8n hoy: los 6 porcentajes pelados, mas
        // "nombreArchivo"/"paginasConsultadas" a nivel de todo el grupo
        // (antes se perdian en silencio, ver PolizasDTO).
        String jsonPolizasViejas = """
                [
                  {
                    "success": true,
                    "data": {
                      "fichaTecnica": { "archivo": "test.pdf" },
                      "polizas": {
                        "nombreArchivo": "Anexo 1. Estudio Previo 39118.pdf",
                        "paginasConsultadas": [29, 37],
                        "seriedad": 10,
                        "cumplimiento": 10,
                        "buenManejoAnticipo": 0,
                        "pagoSalarios": 5,
                        "estabilidad": 0,
                        "calidad": 10
                      }
                    }
                  }
                ]
                """;

        ObjectMapper mapper = new ObjectMapper();
        List<N8nAnalisisResponseDTO> resultados =
                mapper.readValue(jsonPolizasViejas, new TypeReference<List<N8nAnalisisResponseDTO>>() {
                });

        PolizasDTO polizas = resultados.get(0).getData().getPolizas();
        assertThat(polizas.getNombreArchivo()).isEqualTo("Anexo 1. Estudio Previo 39118.pdf");
        assertThat(polizas.getPaginasConsultadas()).containsExactly(29, 37);
        assertThat(polizas.getSeriedad().getValor()).isEqualTo(10);
        assertThat(polizas.getSeriedad().getArchivo()).isNull();
        assertThat(polizas.getSeriedad().getPagina()).isNull();
        assertThat(polizas.getBuenManejoAnticipo().getValor()).isZero();
        assertThat(polizas.getEstabilidad().getValor()).isZero();
        assertThat(polizas.getCumplimiento().getValor()).isEqualTo(10);
        assertThat(polizas.getPagoSalarios().getValor()).isEqualTo(5);
        assertThat(polizas.getCalidad().getValor()).isEqualTo(10);
    }

    @Test
    void paginaComoArregloEnUnItemDeDocumentacionNoRompeElArrayCompletoYConservaTodasLasPaginas() throws JsonProcessingException {
        // Reproduce el error real: "Cannot deserialize value of type
        // java.lang.Integer from Array value", que RestTemplate reportaba
        // como el generico "Error while extracting response for type
        // java.util.List<N8nAnalisisResponseDTO>" (502) sin decir cual campo
        // fallaba. Antes del arreglo, este mapper.readValue lanzaba
        // JsonProcessingException; ahora "pagina": [8, 12] deserializa sin
        // error y CONSERVA ambas paginas (ver PaginaListaFlexibleDeserializer).
        String jsonConPaginaArreglo = JSON_RESPUESTA_N8N.replaceFirst(
                "\"documento\": \"Formato Acreditación Emprendimiento y Empresa de Mujeres\", \"pagina\": 8",
                "\"documento\": \"Formato Acreditación Emprendimiento y Empresa de Mujeres\", \"pagina\": [8, 12]");

        ObjectMapper mapper = new ObjectMapper();
        List<N8nAnalisisResponseDTO> resultados =
                mapper.readValue(jsonConPaginaArreglo, new TypeReference<List<N8nAnalisisResponseDTO>>() {
                });

        assertThat(resultados.get(0).getData().getDocumentacion().getCapacidadJuridica().get(0).getPagina())
                .containsExactly(8, 12);
    }

    private AnalisisDataDTO leerPrimerData() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        List<N8nAnalisisResponseDTO> resultados =
                mapper.readValue(JSON_RESPUESTA_N8N, new TypeReference<List<N8nAnalisisResponseDTO>>() {
                });
        return resultados.get(0).getData();
    }

    private FichaTecnicaDTO leerPrimerFichaTecnica() throws JsonProcessingException {
        return leerPrimerData().getFichaTecnica();
    }

    private DocumentacionDTO leerPrimerDocumentacion() throws JsonProcessingException {
        return leerPrimerData().getDocumentacion();
    }
}
