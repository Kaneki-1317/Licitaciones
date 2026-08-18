package com.licitaciones.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.licitaciones.backend.util.PaginaFlexibleDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Indicadores financieros de la ficha tecnica (requisitosFinancieros).
 *
 * n8n envia las claves en PascalCase (IndiceLiquidez, ROE, ROA...), distinto
 * a la convencion camelCase del resto del contrato. @JsonAlias solo afecta
 * la deserializacion (lectura desde n8n): la respuesta que este backend
 * expone al frontend sigue serializando en camelCase (indiceLiquidez, roe,
 * roa...), que es el contrato estable que React consume.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequisitosFinancierosDTO {

    @JsonAlias("IndiceLiquidez")
    private BigDecimal indiceLiquidez;

    @JsonAlias("NivelEndeudamiento")
    private BigDecimal nivelEndeudamiento;

    @JsonAlias("CoberturaIntereses")
    private BigDecimal coberturaIntereses;

    @JsonAlias("ROE")
    private BigDecimal roe;

    @JsonAlias("ROA")
    private BigDecimal roa;

    /** Pagina de donde n8n extrajo estos indicadores; ExcelService no la usa (solo escribe "valor"), es para trazabilidad. */
    @JsonDeserialize(using = PaginaFlexibleDeserializer.class)
    private Integer pagina;
}
