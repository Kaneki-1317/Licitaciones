package com.licitaciones.backend.dto.response;

import com.licitaciones.backend.entity.EstadoProceso;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta minima para la consulta de estado de un proceso.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadoResponseDTO {

    private EstadoProceso estado;
}
