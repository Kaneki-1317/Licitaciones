package com.licitaciones.backend.service;

import com.licitaciones.backend.entity.Rol;
import com.licitaciones.backend.entity.Usuario;
import com.licitaciones.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Operaciones sobre {@link Usuario}. Dos formas de crear un usuario:
 * {@link #crearAdminSiNoExiste} (el administrador inicial, invocado
 * exclusivamente por {@code config.AdminUsuarioInitializer} al arrancar la
 * aplicacion) y {@link #crearUsuario} (usada por
 * {@code controller.UsuarioController}, POST /api/usuarios, restringido a
 * ADMIN en {@code security.SecurityConfig}). No hay registro publico: ver
 * auth.AuthController, cuyo unico endpoint es POST /api/auth/login.
 *
 * La contrasena se guarda TAL CUAL se recibe, en texto plano (decision
 * explicita del proyecto, sin BCrypt ni ningun otro hashing).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    /** Usado por auth.AuthService para validar credenciales en el login. */
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    /**
     * Crea el usuario administrador si todavia no existe un usuario con ese
     * username. Si ya existe, no lo toca (no resetea la contrasena en cada
     * arranque de la aplicacion).
     */
    @Transactional
    public void crearAdminSiNoExiste(String username, String passwordPlano) {
        if (usuarioRepository.existsByUsername(username)) {
            log.info("El usuario administrador '{}' ya existe; no se modifica.", username);
            return;
        }

        Usuario admin = Usuario.builder()
                .username(username)
                .password(passwordPlano)
                .rol(Rol.ADMIN)
                .activo(true)
                .build();
        usuarioRepository.save(admin);
        log.info("Usuario administrador inicial '{}' creado.", username);
    }

    /**
     * Crea un usuario nuevo (username/password/rol), activo por defecto.
     * Vacio si el username ya existe (el controller lo traduce a 409); la
     * contrasena se guarda exactamente como llega, sin transformarla.
     */
    @Transactional
    public Optional<Usuario> crearUsuario(String username, String passwordPlano, Rol rol) {
        if (usuarioRepository.existsByUsername(username)) {
            return Optional.empty();
        }

        Usuario usuario = Usuario.builder()
                .username(username)
                .password(passwordPlano)
                .rol(rol)
                .activo(true)
                .build();
        Usuario creado = usuarioRepository.save(usuario);
        log.info("Usuario '{}' creado con rol {}.", username, rol);
        return Optional.of(creado);
    }
}
