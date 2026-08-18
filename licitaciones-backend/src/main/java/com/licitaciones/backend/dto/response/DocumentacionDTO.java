package com.licitaciones.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Contenido del campo "documentacion" en data.documentacion, tal como lo
 * envia n8n:
 * {@code { "nombreArchivo": "...",
 *          "capacidadJuridica": [{ "id": 1, "documento": "..." }],
 *          "capacidadTecnica": [{ "id": 1, "documento": "..." }],
 *          "criteriosDeEvaluacion": [{ "id": 1, "documento": "..." }],
 *          "recursoHumanoRequerido": [{ "cargo": "...", "cantidad": "...", "perfil": "..." }] } }.
 *
 * Antes "documentacion" era un array plano de items {@code {nombre, obligatorio}};
 * n8n ahora lo envia como un objeto con el nombre del archivo analizado y
 * cuatro categorias propias. capacidadJuridica, capacidadTecnica y
 * criteriosDeEvaluacion comparten la misma forma de item y reutilizan
 * {@link DocumentacionItemDTO}; recursoHumanoRequerido tiene una forma
 * distinta y usa {@link RecursoHumanoDTO}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentacionDTO {

    /**
     * n8n envia esta propiedad como "archivo", no "nombreArchivo". @JsonAlias
     * (mismo patron que RequisitosFinancierosDTO) solo afecta la lectura:
     * acepta "archivo" al deserializar desde n8n, pero al serializar hacia
     * el frontend sigue emitiendo "nombreArchivo" (el nombre que ya
     * consume Resultado.jsx), sin romper ese contrato.
     */
    @JsonAlias("archivo")
    private String nombreArchivo;
    private List<DocumentacionItemDTO> capacidadJuridica = new ArrayList<>();
    private List<DocumentacionItemDTO> capacidadTecnica = new ArrayList<>();
    private List<DocumentacionItemDTO> criteriosDeEvaluacion = new ArrayList<>();
    private List<RecursoHumanoDTO> recursoHumanoRequerido = new ArrayList<>();
}
