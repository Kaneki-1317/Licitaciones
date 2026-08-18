package com.licitaciones.backend.dto.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de deserializacion contra el JSON REAL que envio n8n para el
 * proceso 21 y que producia el 502 "Error while extracting response for
 * type java.util.List<N8nAnalisisResponseDTO>". A diferencia de
 * {@link N8nAnalisisResponseDeserializationTest} (payload de referencia mas
 * chico), este es el payload completo reportado en el ticket, con varios
 * items de "documentacion" cuya "pagina" viene como arreglo
 * ({@code [12,13]}, {@code [17,19]}) en vez de numero simple.
 */
class N8nAnalisisResponseProceso21DeserializationTest {

    private static final String JSON_PROCESO_21 = """
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
                    "valoracion": { "valor": "Precio (600 puntos), Criterios de Calidad (286 puntos), Estímulo a la industria Nacional Colombiana (100 puntos), Personal con discapacidad (10 puntos), Emprendimientos y empresas de mujeres (2 puntos), Condición de Mipyme (2 puntos). TOTAL: 1.000 puntos.", "pagina": 41 },
                    "codigosRUP": { "valor": [81111500, 81111600, 81111900, 81112200], "pagina": [6, 25] },
                    "requisitosFinancieros": { "IndiceLiquidez": 1.3, "NivelEndeudamiento": 0.7, "CoberturaIntereses": 1, "ROE": 0.04, "ROA": 0.02, "pagina": 35 }
                  },
                  "documentacion": {
                    "archivo": "Anexo 1. Estudio Previo 39118.pdf",
                    "capacidadJuridica": [
                      { "id": 1, "documento": "Acreditación de Emprendimientos y Empresas de Mujeres (Formato Acreditación Emprendimiento y Empresa de Mujeres)", "pagina": 8 },
                      { "id": 2, "documento": "Acreditación de la condición de Mipyme (Formato Acreditación de Mipyme)", "pagina": 10 },
                      { "id": 3, "documento": "Sucursal en el Área Metropolitana", "pagina": 15 },
                      { "id": 4, "documento": "Aval de la propuesta por un profesional en ingeniería de sistemas, electrónica o carreras afines", "pagina": 17 }
                    ],
                    "capacidadTecnica": [
                      { "id": 1, "documento": "Registro Único de Proponentes (RUP)", "pagina": 7 },
                      { "id": 2, "documento": "Experiencia General del Proponente (hasta 5 contratos ejecutados certificados en el RUP en al menos dos de los códigos UNSPSC solicitados)", "pagina": 7 },
                      { "id": 3, "documento": "Documentos válidos para la acreditación de la experiencia requerida (Certificaciones de experiencia, Acta de liquidación)", "pagina": [12, 13] },
                      { "id": 4, "documento": "Formato Certificación Clasificación UNSPSC para Experiencia del Proponente Extranjero", "pagina": 15 }
                    ],
                    "criteriosDeEvaluacion": [
                      { "id": 1, "documento": "Precio (600 puntos)", "pagina": [17, 19] },
                      { "id": 2, "documento": "Calidad (286 puntos)", "pagina": [17, 19] },
                      { "id": 3, "documento": "Estímulo a la industria nacional (100 puntos)", "pagina": 17 },
                      { "id": 4, "documento": "Discapacidad (10 puntos)", "pagina": 17 },
                      { "id": 5, "documento": "Emprendimiento de empresas y mujeres (2 puntos)", "pagina": 17 },
                      { "id": 6, "documento": "Condición Mipyme (2 puntos)", "pagina": 17 }
                    ]
                  },
                  "perfiles": [
                    {
                      "archivo": "Anexo 1. Estudio Previo 39118.pdf",
                      "pagina": 16,
                      "cargos": [
                        { "cargo": "Desarrolladores FullStack", "cantidad": 16, "perfil": "Ingeniero de sistemas y/o tecnólogo en sistemas, Ingeniero electrónico o carreras afines a la NBC titulado con más de 2 años de experiencia profesional y experiencia en participación en proyectos de desarrollo de software." },
                        { "cargo": "Líder User Experiencia - UX", "cantidad": 2, "perfil": "Profesional universitario en áreas afines al diseño gráfico, titulado con más de 2 años de experiencia interactuando con usuarios finales." },
                        { "cargo": "Ingeniero QA # Automatización", "cantidad": 4, "perfil": "Profesional en Sistemas, Ingeniero electrónico o carreras afines a la NBC con mínimo 2 años de experiencia profesional en la ejecución de pruebas de calidad en sistemas de información." },
                        { "cargo": "Senior SME (Arquitecto)", "cantidad": 2, "perfil": "Ingeniero de sistemas, electrónico o carreras afines a la NBC titulado con más de 5 años de experiencia profesional liderando en proyectos de desarrollo de software." },
                        { "cargo": "Ingeniero de Sistemas", "cantidad": 2, "perfil": "Ingeniero de sistemas, electrónico o carreras afines a la NBC titulado con más de 2 años de experiencia en tecnologías CMS como Wordpress, Dkan, Drupal, Shopify, Wix y Squarespace" },
                        { "cargo": "Gerente de proyectos", "cantidad": 1, "perfil": "Ingeniero de Sistemas, industrial, electrónico o carreras afines a la NBC, con Especialización en Gerencia de proyectos; con mínimo 5 años de experiencia profesional en la ejecución de proyectos en el área de tecnología." },
                        { "cargo": "Analista de Requisitos", "cantidad": 2, "perfil": "Ingeniero de sistemas y/o tecnólogo en sistemas, Ingeniero electrónico o carreras afines a la NBC titulado con más de 1 año de experiencia profesional y experiencia en participación en proyectos de desarrollo de software." },
                        { "cargo": "Ingeniero Especialista para la nube de Azure", "cantidad": 1, "perfil": "Ingeniero de sistemas, electrónico o carreras afines a la NBC titulado con más de 5 años de experiencia profesional liderando en proyectos de desarrollo de software en nube de AZURE." }
                      ]
                    }
                  ],
                  "polizas": null
                }
              }
            ]
            """;

    @Test
    void deserializaElPayloadCompletoDelProceso21() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        List<N8nAnalisisResponseDTO> resultado =
                mapper.readValue(JSON_PROCESO_21, new TypeReference<List<N8nAnalisisResponseDTO>>() {
                });

        assertNotNull(resultado);
        assertEquals(1, resultado.size());

        assertTrue(resultado.get(0).getSuccess());
        assertNotNull(resultado.get(0).getData());

        assertNotNull(resultado.get(0).getData().getFichaTecnica());
        assertNotNull(resultado.get(0).getData().getDocumentacion());
        assertNotNull(resultado.get(0).getData().getPerfiles());

        assertEquals(
                2424588580L,
                resultado.get(0).getData().getFichaTecnica().getPresupuesto().getValor().longValue());

        assertThat(resultado.get(0).getData().getDocumentacion().getCapacidadJuridica().get(0).getPagina())
                .containsExactly(8);
    }

    @Test
    void paginaComoArregloEnCapacidadTecnicaConservaTodasLasPaginas() throws JsonProcessingException {
        DocumentacionDTO documentacion = leerPrimeraDocumentacion();

        // id 3: "pagina": [12, 13]
        DocumentacionItemDTO item = documentacion.getCapacidadTecnica().get(2);
        assertThat(item.getId()).isEqualTo(3);
        assertThat(item.getPagina()).containsExactly(12, 13);
    }

    @Test
    void paginaComoArregloEnCriteriosDeEvaluacionConservaTodasLasPaginas() throws JsonProcessingException {
        DocumentacionDTO documentacion = leerPrimeraDocumentacion();

        // id 1 y 2: "pagina": [17, 19]
        assertThat(documentacion.getCriteriosDeEvaluacion().get(0).getPagina()).containsExactly(17, 19);
        assertThat(documentacion.getCriteriosDeEvaluacion().get(1).getPagina()).containsExactly(17, 19);
        // id 3..6 siguen siendo numero simple -> se normalizan a lista de un elemento, sin regresion
        assertThat(documentacion.getCriteriosDeEvaluacion().get(2).getPagina()).containsExactly(17);
    }

    @Test
    void codigosRupYRequisitosFinancierosSeMapeanCompletos() throws JsonProcessingException {
        FichaTecnicaDTO ficha = leerPrimeraFichaTecnica();

        assertThat(ficha.getCodigosRUP().getValor()).containsExactly(81111500L, 81111600L, 81111900L, 81112200L);
        assertThat(ficha.getCodigosRUP().getPagina()).containsExactly(6, 25);

        RequisitosFinancierosDTO indicadores = ficha.getRequisitosFinancieros();
        assertThat(indicadores.getIndiceLiquidez()).isEqualByComparingTo("1.3");
        assertThat(indicadores.getRoe()).isEqualByComparingTo("0.04");
        assertThat(indicadores.getPagina()).isEqualTo(35);
    }

    @Test
    void perfilesYCargosSeMapeanCompletos() throws JsonProcessingException {
        List<PerfilDTO> perfiles = leerPrimerData().getPerfiles();

        assertThat(perfiles).hasSize(1);
        PerfilDTO grupo = perfiles.get(0);
        assertThat(grupo.getPagina()).isEqualTo(16);
        assertThat(grupo.getCargos()).hasSize(8);
        assertThat(grupo.getCargos().get(0).getCargo()).isEqualTo("Desarrolladores FullStack");
        assertThat(grupo.getCargos().get(0).getCantidad()).isEqualTo(16);
    }

    @Test
    void polizasNullNoRompeLaDeserializacion() throws JsonProcessingException {
        assertThat(leerPrimerData()).isNotNull();
    }

    private AnalisisDataDTO leerPrimerData() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        List<N8nAnalisisResponseDTO> resultados =
                mapper.readValue(JSON_PROCESO_21, new TypeReference<List<N8nAnalisisResponseDTO>>() {
                });
        return resultados.get(0).getData();
    }

    private FichaTecnicaDTO leerPrimeraFichaTecnica() throws JsonProcessingException {
        return leerPrimerData().getFichaTecnica();
    }

    private DocumentacionDTO leerPrimeraDocumentacion() throws JsonProcessingException {
        return leerPrimerData().getDocumentacion();
    }
}
