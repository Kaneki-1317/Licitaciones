package com.licitaciones.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta de un usuario creado (201, POST /api/usuarios). Deliberadamente
 * NUNCA incluye "password" (aunque hoy se guarde en texto plano, no hay
 * motivo para hacerla viajar de vuelta en la respuesta).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioResponseDTO {

    private Long id;
    private String username;
    private String rol;
    private boolean activo;
}
