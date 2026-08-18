package com.licitaciones.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Contenido del campo "data" en la respuesta de n8n
 * ({@code { "success": true, "data": { "fichaTecnica": {...}, "documentacion": {...}, "perfiles": [...] } } }).
 *
 * Ficha Tecnica, Documentacion, Perfiles y Polizas son los modulos
 * implementados actualmente en n8n; fuentes queda preparado (lista vacia por
 * defecto) para cuando ese flujo de analisis exista. perfiles tambien tiene
 * lista vacia por defecto: los resultados guardados antes de que este campo
 * existiera (sin "perfiles" en su JSON) deserializan a lista vacia en vez de
 * fallar. polizas queda en null si n8n no lo envia (resultados guardados
 * antes de que este campo existiera, o payload con "polizas": null); ver
 * {@link PolizasDTO} para la forma de cada poliza individual.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalisisDataDTO {

    private FichaTecnicaDTO fichaTecnica;
    private DocumentacionDTO documentacion;
    private List<PerfilDTO> perfiles = new ArrayList<>();
    private PolizasDTO polizas;
    private List<FuenteDTO> fuentes = new ArrayList<>();
}
