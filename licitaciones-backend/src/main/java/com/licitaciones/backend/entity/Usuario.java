package com.licitaciones.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Usuario con acceso a la aplicacion. El unico registro publico es
 * POST /api/auth/login (auth.AuthController); crear usuarios nuevos
 * requiere estar autenticado como ADMIN (ver controller.UsuarioController).
 * El administrador inicial lo crea {@code config.AdminUsuarioInitializer}
 * al arrancar la aplicacion a partir de las variables de entorno
 * ADMIN_USERNAME/ADMIN_PASSWORD.
 *
 * "password" se guarda y se compara TAL CUAL, en texto plano (decision
 * explicita del proyecto: ver auth.AuthService, que hace
 * {@code usuario.getPassword().equals(passwordRecibida)} sin BCrypt ni
 * ningun otro hashing). Nunca se serializa hacia el frontend (ni
 * LoginResponseDTO/UsuarioResponseDTO ni ningun otro DTO exponen este
 * campo).
 */
@Entity
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(name = "uk_usuarios_username", columnNames = "username"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    /** Texto plano, tal como la escribe el usuario. Ver la nota de la clase. */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /**
     * columnDefinition explicito: MySQL ya tenia esta columna como ENUM
     * nativo ('ADMIN'); Hibernate con ddl-auto=update NO amplia el set de
     * valores de un ENUM nativo existente al agregar un rol nuevo (USER), asi
     * que se fuerza VARCHAR (una columna de texto simple, sin lista fija de
     * valores en la base de datos) para poder agregar roles futuros sin
     * volver a tocar el esquema a mano.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private Rol rol;

    /** Permite desactivar el acceso de un usuario sin borrar el registro. */
    @Column(name = "activo", nullable = false)
    private boolean activo;
}
