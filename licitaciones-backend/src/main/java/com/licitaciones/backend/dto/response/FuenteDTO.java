package com.licitaciones.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Trazabilidad de un campo encontrado por n8n: en que archivo y pagina se
 * hallo la informacion, para permitir su verificacion posterior.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FuenteDTO {

    private String campo;
    private String archivo;
    private Integer pagina;
}
