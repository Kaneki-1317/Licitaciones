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
 * Contenido del campo "polizas" en data.polizas: los seis tipos de
 * poliza/garantia que exige el proceso, cada uno con su porcentaje y
 * trazabilidad (ver {@link PolizaDTO}), mas dos datos de trazabilidad a
 * nivel de todo el grupo que n8n ya envia hoy junto a los seis porcentajes:
 * {@code { "nombreArchivo": "...", "paginasConsultadas": [29, 37],
 *          "seriedad": {...}, "cumplimiento": {...}, "buenManejoAnticipo": {...},
 *          "pagoSalarios": {...}, "estabilidad": {...}, "calidad": {...} } }.
 *
 * Antes de agregar estos dos campos, "nombreArchivo" y "paginasConsultadas"
 * no tenian propiedad destino en esta clase; al ser {@code @JsonIgnoreProperties}
 * se descartaban en silencio durante la deserializacion. "paginasConsultadas"
 * conserva TODAS las paginas del arreglo (ver {@link PaginaListaFlexibleDeserializer}),
 * igual que {@code DocumentacionItemDTO.pagina}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PolizasDTO {

    private String nombreArchivo;

    @JsonDeserialize(using = PaginaListaFlexibleDeserializer.class)
    private List<Integer> paginasConsultadas;

    private PolizaDTO seriedad;
    private PolizaDTO cumplimiento;
    private PolizaDTO buenManejoAnticipo;
    private PolizaDTO pagoSalarios;
    private PolizaDTO estabilidad;
    private PolizaDTO calidad;
}
