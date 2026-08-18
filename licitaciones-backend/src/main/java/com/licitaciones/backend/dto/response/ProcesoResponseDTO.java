package com.licitaciones.backend.dto.response;

import com.licitaciones.backend.entity.EstadoProceso;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Representacion de un proceso de licitacion expuesta al frontend.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcesoResponseDTO {

    private Long id;
    private String nombreProceso;
    private EstadoProceso estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaFinalizacion;
    private String archivoGenerado;
}
