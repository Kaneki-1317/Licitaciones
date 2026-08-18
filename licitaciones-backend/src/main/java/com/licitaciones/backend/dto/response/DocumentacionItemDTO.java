package com.licitaciones.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.licitaciones.backend.util.PaginaListaFlexibleDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Item individual dentro de una de las categorias de
 * {@link DocumentacionDTO} (capacidadJuridica, capacidadTecnica,
 * criteriosDeEvaluacion), tal como lo envia n8n:
 * {@code { "id": 1, "documento": "Registro Unico de Proponentes (RUP)", "pagina": 8 } }.
 * Cuando el documento aparece en mas de una pagina, n8n envia
 * {@code "pagina": [7, 15]} en su lugar; a diferencia de otros campos del
 * contrato (fichaTecnica, perfiles) aqui se CONSERVAN todas las paginas del
 * arreglo, no solo la primera — ver {@link PaginaListaFlexibleDeserializer}.
 *
 * Reutilizada por las tres categorias porque comparten exactamente esta
 * misma forma; antes representaba un item plano de documentacion[]
 * ({@code {nombre, obligatorio}}), forma que n8n ya no envia.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentacionItemDTO {

    private Integer id;
    private String documento;

    /**
     * Pagina(s) del documento fuente de donde n8n extrajo este item
     * (trazabilidad). Acepta tanto {@code "pagina": 15} (se normaliza a
     * {@code [15]}) como {@code "pagina": [7, 15]} (se conservan ambas);
     * queda {@code null} si no vino informacion valida, sin inventar nada.
     */
    @JsonDeserialize(using = PaginaListaFlexibleDeserializer.class)
    private List<Integer> pagina;
}
