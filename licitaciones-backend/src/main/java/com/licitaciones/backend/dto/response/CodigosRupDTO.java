package com.licitaciones.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * fichaTecnica.codigosRUP, tal como lo envia n8n:
 * {@code { "valor": [81111500, 81111600], "pagina": [6, 25] } }.
 *
 * A diferencia del resto de campos de fichaTecnica (ver {@link CampoTrazableDTO},
 * donde "pagina" es un unico Integer), aqui "pagina" es su propia lista: una
 * entrada por cada pagina donde n8n encontro codigos, no necesariamente del
 * mismo tamano que "valor". Por eso codigosRUP tiene esta forma dedicada en
 * vez de reutilizar {@code CampoTrazableDTO<List<Long>>}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CodigosRupDTO {

    private List<Long> valor;
    private List<Integer> pagina;
}
