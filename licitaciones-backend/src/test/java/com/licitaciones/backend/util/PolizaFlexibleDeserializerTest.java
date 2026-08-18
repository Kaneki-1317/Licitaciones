package com.licitaciones.backend.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.licitaciones.backend.dto.response.PolizaDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PolizaFlexibleDeserializer} en aislamiento: cubre el contrato
 * nuevo ({@code {valor, archivo, pagina}}), el contrato viejo (numero
 * "pelado", sin trazabilidad) y el caso "polizas": null completo.
 */
class PolizaFlexibleDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void aceptaContratoNuevoConValorArchivoYPagina() throws JsonProcessingException {
        PolizaDTO poliza = mapper.readValue(
                "{ \"valor\": 10, \"archivo\": \"anexo.pdf\", \"pagina\": 42 }", PolizaDTO.class);

        assertThat(poliza.getValor()).isEqualTo(10);
        assertThat(poliza.getArchivo()).isEqualTo("anexo.pdf");
        assertThat(poliza.getPagina()).isEqualTo(42);
    }

    @Test
    void preservaValorCeroComoDatoValido() throws JsonProcessingException {
        // 0 significa "poliza no exigida", no "dato ausente".
        PolizaDTO poliza = mapper.readValue(
                "{ \"valor\": 0, \"archivo\": \"anexo.pdf\", \"pagina\": 43 }", PolizaDTO.class);

        assertThat(poliza.getValor()).isZero();
    }

    @Test
    void aceptaPaginaComoArregloYTomaElPrimerElemento() throws JsonProcessingException {
        PolizaDTO poliza = mapper.readValue(
                "{ \"valor\": 5, \"archivo\": \"anexo.pdf\", \"pagina\": [43, 44] }", PolizaDTO.class);

        assertThat(poliza.getPagina()).isEqualTo(43);
    }

    @Test
    void aceptaContratoViejoDeNumeroPeladoSinArchivoNiPagina() throws JsonProcessingException {
        // Compatibilidad con lo que n8n envia hoy: "seriedad": 10.
        PolizaDTO poliza = mapper.readValue("10", PolizaDTO.class);

        assertThat(poliza.getValor()).isEqualTo(10);
        assertThat(poliza.getArchivo()).isNull();
        assertThat(poliza.getPagina()).isNull();
    }

    @Test
    void aceptaContratoViejoConValorCero() throws JsonProcessingException {
        PolizaDTO poliza = mapper.readValue("0", PolizaDTO.class);

        assertThat(poliza.getValor()).isZero();
        assertThat(poliza.getArchivo()).isNull();
        assertThat(poliza.getPagina()).isNull();
    }

    @Test
    void aceptaPolizaNula() throws JsonProcessingException {
        PolizaDTO poliza = mapper.readValue("null", PolizaDTO.class);

        assertThat(poliza).isNull();
    }

    @Test
    void archivoYPaginaAusentesEnElObjetoQuedanNulosSinInventarDatos() throws JsonProcessingException {
        PolizaDTO poliza = mapper.readValue("{ \"valor\": 10 }", PolizaDTO.class);

        assertThat(poliza.getValor()).isEqualTo(10);
        assertThat(poliza.getArchivo()).isNull();
        assertThat(poliza.getPagina()).isNull();
    }
}
