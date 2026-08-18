package com.licitaciones.backend.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.licitaciones.backend.util.PolizaFlexibleDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una poliza/garantia individual dentro de data.polizas (ej. "seriedad",
 * "cumplimiento"), con el porcentaje exigido y su trazabilidad:
 * {@code { "valor": 10, "archivo": "...", "pagina": 42 } }.
 *
 * "valor" es el porcentaje requerido; 0 es un valor valido (poliza no
 * exigida) y se preserva tal cual, nunca se trata como dato ausente.
 * "archivo"/"pagina" quedan en {@code null} cuando n8n aun no los envia (ver
 * {@link PolizaFlexibleDeserializer}, que tambien acepta el contrato viejo
 * donde la poliza es un numero "pelado" sin trazabilidad) — el frontend
 * muestra "No disponible" en ese caso, sin inventar la fuente.
 *
 * La deserializacion (lectura de JSON) la resuelve por completo
 * {@link PolizaFlexibleDeserializer}, por eso esta clase no necesita
 * {@code @JsonIgnoreProperties}: el deserializador nunca lee propiedades que
 * no conoce. La serializacion (escritura hacia el frontend) si usa los
 * getters/setters estandar de Lombok, emitiendo siempre las tres
 * propiedades.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonDeserialize(using = PolizaFlexibleDeserializer.class)
public class PolizaDTO {

    private Integer valor;
    private String archivo;
    private Integer pagina;
}
