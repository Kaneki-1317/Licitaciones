package com.licitaciones.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.licitaciones.backend.util.PaginaFlexibleDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Grupo de perfiles/cargos requeridos encontrados en un archivo y pagina
 * puntuales, tal como lo envia n8n en data.perfiles:
 * {@code { "archivo": "Anexo 1. Estudio Previo 39118.pdf", "pagina": 16,
 *          "cargos": [{ "cargo": "...", "cantidad": 16, "perfil": "..." }] } }.
 *
 * archivo/pagina son la fuente de trazabilidad de todo el grupo (no se
 * repiten por cargo); cargos trae la lista de perfiles requeridos
 * encontrados en esa pagina.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PerfilDTO {

    private String archivo;

    /** Acepta tanto {@code "pagina": 16} como {@code "pagina": [16, 18]}; ver {@link PaginaFlexibleDeserializer}. */
    @JsonDeserialize(using = PaginaFlexibleDeserializer.class)
    private Integer pagina;
    private List<CargoDTO> cargos = new ArrayList<>();
}
