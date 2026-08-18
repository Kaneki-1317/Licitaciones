package com.licitaciones.backend.entity;

/**
 * Roles posibles de un {@link Usuario}. Viaja como claim "role" en el JWT
 * (ver {@code auth.JwtService}) y {@code auth.JwtAuthenticationFilter} lo
 * convierte en la autoridad de Spring Security {@code "ROLE_" + name()}
 * (ej. {@code ADMIN} -> {@code ROLE_ADMIN}).
 *
 * ADMIN es el unico rol que puede crear usuarios (ver
 * {@code controller.UsuarioController}, protegido con
 * {@code hasRole("ADMIN")} en {@code security.SecurityConfig}). USER es un
 * usuario normal sin ese permiso.
 */
public enum Rol {

    ADMIN,
    USER
}
