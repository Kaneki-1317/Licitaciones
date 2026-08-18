package com.licitaciones.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta de un login fallido (401): mensaje generico que NUNCA revela
 * si el usuario existe o si fue la contrasena la que fallo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginErrorResponseDTO {

    private boolean success;
    private String message;
}
