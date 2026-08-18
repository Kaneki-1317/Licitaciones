package com.licitaciones.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.licitaciones.backend.util.PaginaFlexibleDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Envoltorio generico para un campo de fichaTecnica con trazabilidad de
 * pagina, tal como lo envia n8n:
 * {@code { "valor": ..., "pagina": 3 } }.
 *
 * T es el tipo real del valor extraido (String, BigDecimal, etc.); Jackson
 * lo resuelve automaticamente a partir del tipo declarado en cada campo de
 * {@link FichaTecnicaDTO} (ej. {@code CampoTrazableDTO<BigDecimal> presupuesto}),
 * sin necesidad de anotaciones adicionales.
 *
 * No confundir con {@link CodigosRupDTO}: ahi "pagina" es una lista propia
 * (una entrada por pagina, no necesariamente del mismo tamano que "valor"),
 * por eso codigosRUP no reutiliza este wrapper generico.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampoTrazableDTO<T> {

    private T valor;

    /** Acepta tanto {@code "pagina": 3} como {@code "pagina": [3, 7]}; ver {@link PaginaFlexibleDeserializer}. */
    @JsonDeserialize(using = PaginaFlexibleDeserializer.class)
    private Integer pagina;
}
