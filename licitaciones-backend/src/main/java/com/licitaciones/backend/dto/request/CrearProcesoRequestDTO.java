package com.licitaciones.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos de entrada para crear un nuevo proceso de licitacion.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CrearProcesoRequestDTO {

    @NotBlank(message = "El nombre del proceso es obligatorio")
    @Size(max = 255, message = "El nombre del proceso no puede superar 255 caracteres")
    private String nombreProceso;
}
