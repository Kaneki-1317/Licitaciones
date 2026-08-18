package com.licitaciones.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta de un login exitoso (POST /api/auth/login). Nunca incluye la
 * contrasena ni el hash: solo el JWT y los datos que el frontend necesita
 * para la sesion (ver AuthContext en React).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {

    private boolean success;
    private String message;
    private String token;
    private String username;
    private String role;
}
