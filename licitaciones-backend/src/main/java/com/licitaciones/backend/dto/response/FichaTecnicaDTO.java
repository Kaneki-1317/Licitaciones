package com.licitaciones.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Ficha tecnica del proceso, tal como la extrae n8n de los documentos
 * (pliego de condiciones, estudios previos, etc.). Nombres de propiedad
 * identicos a los que envia n8n en data.fichaTecnica.
 *
 * n8n envuelve cada valor extraido junto con la pagina de la que salio
 * (trazabilidad): {@code { "valor": ..., "pagina": N } }, de ahi que la
 * mayoria de estos campos sean {@link CampoTrazableDTO} y no el tipo
 * "pelado" directamente. "archivo" es la unica excepcion simple (nombre del
 * documento fuente, un solo valor para toda la ficha, sin pagina asociada);
 * "codigosRUP" tiene su propia forma porque su "pagina" es una lista (ver
 * {@link CodigosRupDTO}); "requisitosFinancieros" tampoco usa este wrapper,
 * ver {@link RequisitosFinancierosDTO}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FichaTecnicaDTO {

    private String archivo;
    private CampoTrazableDTO<String> nombreProyecto;
    private CampoTrazableDTO<String> entidad;

    /** n8n aun no envia este campo (no viene envuelto en {valor,pagina}); queda listo para cuando lo incluya. */
    private String numeroProceso;

    private CampoTrazableDTO<String> objetoContrato;
    private CampoTrazableDTO<BigDecimal> presupuesto;
    private CampoTrazableDTO<String> plazoProyecto;
    private CampoTrazableDTO<String> ciudad;
    private CampoTrazableDTO<String> valoracion;
    private CodigosRupDTO codigosRUP;
    private RequisitosFinancierosDTO requisitosFinancieros;
}
