package com.licitaciones.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cargo individual dentro de un grupo de {@link PerfilDTO}, tal como lo
 * envia n8n:
 * {@code { "cargo": "Desarrolladores FullStack", "cantidad": 16,
 *          "perfil": "Ingeniero de sistemas..." } }.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CargoDTO {

    private String cargo;
    private Integer cantidad;
    private String perfil;
}
