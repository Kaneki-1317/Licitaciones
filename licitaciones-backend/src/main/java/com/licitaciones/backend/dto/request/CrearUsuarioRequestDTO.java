package com.licitaciones.backend.dto.request;

import com.licitaciones.backend.entity.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos de entrada para crear un usuario nuevo (POST /api/usuarios, solo
 * ADMIN). "password" viaja y se guarda tal cual, en texto plano (ver
 * auth.AuthService/service.UsuarioService); "rol" es uno de los valores de
 * {@link Rol} (hoy ADMIN o USER); "activo" no se recibe aqui, siempre queda
 * en {@code true} por defecto (ver service.UsuarioService#crearUsuario).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CrearUsuarioRequestDTO {

    @NotBlank(message = "El usuario es obligatorio")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    @NotNull(message = "El rol es obligatorio")
    private Rol rol;
}
