package com.licitaciones.backend.dto.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AUDITORIA FORENSE: aisla la variable "pagina" de todo lo demas (transporte
 * HTTP, N8nService, controller) para responder una pregunta puntual: ¿el
 * campo "pagina" -por si solo, a nivel de tipos Java- es capaz de convertir
 * una deserializacion exitosa en una fallida?
 *
 * Los 4 tests de esta clase deserializan CUATRO variantes del MISMO array
 * raiz (siempre {@code [ {...} ] }, nunca {@code {...} }) usando el mismo
 * ObjectMapper por defecto que usa N8nClient (sin configuracion custom):
 *
 *  A) contrato viejo, hipotetico, SIN "pagina" en ningun campo
 *  B) contrato actual, CON "pagina" como Integer simple
 *  C) contrato actual, CON "pagina" como arreglo de un elemento ([8])
 *  D) contrato actual, CON "pagina" como arreglo de varios elementos ([7,12,13,15])
 *
 * Si "pagina" fuera la causa de que el backend reciba START_OBJECT en la
 * raiz, alguno de estos 4 tests tendria que fallar con
 * MismatchedInputException apuntando a la raiz (List). Si los 4 pasan,
 * queda demostrado que "pagina" no es capaz de alterar el tipo de la raiz
 * del documento -ni el numero de elementos del array raiz-, porque un
 * JsonDeserializer de un campo interno (Integer) no tiene ninguna forma de
 * tocar el token que Jackson ve ANTES de empezar a leer ese campo.
 */
class N8nContratoRegresionTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testA_contratoViejoHipoteticoSinPaginaFunciona() throws JsonProcessingException {
        // TEST A del pedido: version "antes de agregar pagina" (hipotetica,
        // no hay evidencia en el repo de que esta forma haya existido
        // realmente -ver auditoria de git de la vuelta anterior-, pero se
        // reproduce tal cual la pide el pedido para descartarla como causa).
        String jsonSinPagina = """
                [
                  {
                    "success": true,
                    "data": {
                      "fichaTecnica": {
                        "archivo": "test.pdf",
                        "nombreProyecto": { "valor": "123" }
                      }
                    }
                  }
                ]
                """;

        List<N8nAnalisisResponseDTO> resultado = deserializar(jsonSinPagina);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getSuccess()).isTrue();
        assertThat(resultado.get(0).getData().getFichaTecnica().getArchivo()).isEqualTo("test.pdf");
        assertThat(resultado.get(0).getData().getFichaTecnica().getNombreProyecto().getValor()).isEqualTo("123");
        // "pagina" no vino en el JSON -> el campo queda null, sin error.
        assertThat(resultado.get(0).getData().getFichaTecnica().getNombreProyecto().getPagina()).isNull();
    }

    @Test
    void testB_contratoActualConPaginaEnteroFunciona() throws JsonProcessingException {
        // TEST B del pedido: mismo payload, agregando "pagina": 1 (Integer).
        String jsonConPaginaEntero = """
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

        List<N8nAnalisisResponseDTO> resultado = deserializar(jsonConPaginaEntero);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getData().getFichaTecnica().getNombreProyecto().getValor()).isEqualTo("123");
        assertThat(resultado.get(0).getData().getFichaTecnica().getNombreProyecto().getPagina()).isEqualTo(1);
    }

    @Test
    void testC_paginaComoArregloDeUnElementoFunciona() throws JsonProcessingException {
        // Variante real del payload pegado en este pedido: capacidadJuridica
        // trae "pagina": [8] (arreglo de UN solo elemento, no un entero
        // simple ni un arreglo de varios).
        String jsonPaginaArregloUnElemento = """
                [
                  {
                    "success": true,
                    "data": {
                      "documentacion": {
                        "archivo": "anexo.pdf",
                        "capacidadJuridica": [
                          { "id": 1, "documento": "RUP", "pagina": [8] }
                        ],
                        "capacidadTecnica": [],
                        "criteriosDeEvaluacion": []
                      }
                    }
                  }
                ]
                """;

        List<N8nAnalisisResponseDTO> resultado = deserializar(jsonPaginaArregloUnElemento);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getData().getDocumentacion().getCapacidadJuridica().get(0).getPagina())
                .containsExactly(8);
    }

    @Test
    void testD_paginaComoArregloDeVariosElementosFunciona() throws JsonProcessingException {
        // Variante real del payload pegado en este pedido: capacidadTecnica
        // trae "pagina": [7,12,13,15] (arreglo de 4 elementos).
        String jsonPaginaArregloVarios = """
                [
                  {
                    "success": true,
                    "data": {
                      "documentacion": {
                        "archivo": "anexo.pdf",
                        "capacidadJuridica": [],
                        "capacidadTecnica": [
                          { "id": 1, "documento": "Experiencia", "pagina": [7,12,13,15] }
                        ],
                        "criteriosDeEvaluacion": [
                          { "id": 1, "documento": "Precio", "pagina": [17,19] }
                        ]
                      }
                    }
                  }
                ]
                """;

        List<N8nAnalisisResponseDTO> resultado = deserializar(jsonPaginaArregloVarios);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getData().getDocumentacion().getCapacidadTecnica().get(0).getPagina())
                .containsExactly(7, 12, 13, 15);
        assertThat(resultado.get(0).getData().getDocumentacion().getCriteriosDeEvaluacion().get(0).getPagina())
                .containsExactly(17, 19);
    }

    @Test
    void raizObjectFallaIndependientementeDeSiTienePaginaONo() {
        // Contraparte de los 4 anteriores: lo que SI rompe la deserializacion
        // no es "pagina" (que aqui ni siquiera aparece), es la raiz. Se deja
        // explicito para que quede claro que la variable que importa es el
        // primer token del documento, no el contenido interno de "data".
        String jsonRaizObjectSinPagina = """
                { "success": true, "data": { "fichaTecnica": { "archivo": "test.pdf" } } }
                """;

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> deserializar(jsonRaizObjectSinPagina))
                .isInstanceOf(com.fasterxml.jackson.databind.exc.MismatchedInputException.class)
                .hasMessageContaining("START_OBJECT");
    }

    private List<N8nAnalisisResponseDTO> deserializar(String json) throws JsonProcessingException {
        return mapper.readValue(json, new TypeReference<List<N8nAnalisisResponseDTO>>() {
        });
    }
}
