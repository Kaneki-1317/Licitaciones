package com.licitaciones.backend.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PaginaFlexibleDeserializer} en aislamiento: cubre exactamente el
 * escenario que antes rompia el mapeo de List&lt;N8nAnalisisResponseDTO&gt;
 * con "Cannot deserialize value of type java.lang.Integer from Array value"
 * cuando n8n envia "pagina" como arreglo en vez de numero.
 */
class PaginaFlexibleDeserializerTest {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class Contenedor {
        @JsonDeserialize(using = PaginaFlexibleDeserializer.class)
        private Integer pagina;
    }

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void aceptaPaginaComoNumeroSimple() throws JsonProcessingException {
        Contenedor c = mapper.readValue("{ \"pagina\": 5 }", Contenedor.class);
        assertThat(c.getPagina()).isEqualTo(5);
    }

    @Test
    void aceptaPaginaComoArregloYTomaElPrimerElemento() throws JsonProcessingException {
        Contenedor c = mapper.readValue("{ \"pagina\": [17, 19] }", Contenedor.class);
        assertThat(c.getPagina()).isEqualTo(17);
    }

    @Test
    void aceptaPaginaNula() throws JsonProcessingException {
        Contenedor c = mapper.readValue("{ \"pagina\": null }", Contenedor.class);
        assertThat(c.getPagina()).isNull();
    }

    @Test
    void aceptaArregloVacioComoNulo() throws JsonProcessingException {
        Contenedor c = mapper.readValue("{ \"pagina\": [] }", Contenedor.class);
        assertThat(c.getPagina()).isNull();
    }
}
