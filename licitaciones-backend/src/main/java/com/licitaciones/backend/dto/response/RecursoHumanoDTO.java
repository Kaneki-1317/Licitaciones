package com.licitaciones.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Elemento de documentacion.recursoHumanoRequerido, tal como lo envia n8n:
 * {@code { "cargo": "...", "cantidad": "...", "perfil": "..." } }.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecursoHumanoDTO {

    private String cargo;
    private String cantidad;
    private String perfil;
}
