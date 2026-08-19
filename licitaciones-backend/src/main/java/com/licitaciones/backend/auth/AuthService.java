package com.licitaciones.backend.auth;

import com.licitaciones.backend.dto.request.LoginRequestDTO;
import com.licitaciones.backend.dto.response.LoginResponseDTO;
import com.licitaciones.backend.entity.Usuario;
import com.licitaciones.backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Orquesta el login: valida credenciales contra {@link UsuarioService} y,
 * si son correctas, emite el JWT con {@link JwtService}.
 *
 * La contrasena se compara TAL CUAL, en texto plano
 * ({@code usuario.getPassword().equals(passwordRecibida)}), sin BCrypt ni
 * ningun otro hashing: decision explicita del proyecto.
 *
 * Deliberadamente NO usa el {@code AuthenticationManager}/
 * {@code DaoAuthenticationProvider} de Spring Security: con un solo tipo de
 * credencial (username + password contra la tabla "usuarios") esa
 * indireccion no aporta nada y complica devolver exactamente el contrato
 * {success, token, username, role} / {success:false, message} que espera
 * el frontend. Spring Security se usa igual para proteger el resto de la
 * API (ver security.SecurityConfig y JwtAuthenticationFilter).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    /** Mensaje generico de login fallido: usuario inexistente/inactivo y password incorrecto responden igual. */
    public static final String MENSAJE_CREDENCIALES_INVALIDAS = "Credenciales inválidas";

    private static final String MENSAJE_LOGIN_EXITOSO = "Login exitoso";

    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    /**
     * Valida username/password y, si son correctos, devuelve el JWT ya
     * generado. Vacio si el usuario no existe, esta inactivo o la
     * contrasena no coincide: en los tres casos el controller responde el
     * mismo mensaje generico (ver {@link #MENSAJE_CREDENCIALES_INVALIDAS} /
     * AuthController), para no revelar si el usuario existe.
     *
     * Logs de diagnostico: van todos en DEBUG (no se imprimen en produccion
     * salvo que se active explicitamente con LOG_LEVEL=DEBUG), y ninguno
     * imprime la contrasena ni el JWT completo -- solo username, booleanos
     * de estado (encontrado/activo/password-coincide) y la confirmacion de
     * que el token se genero. El unico log que queda visible en produccion
     * (WARN) sigue sin revelar cual de los tres checks fue el que fallo.
     */
    public Optional<LoginResponseDTO> login(LoginRequestDTO request) {
        log.debug("Login: intento recibido para username '{}'", request.getUsername());

        Optional<Usuario> usuario = usuarioService.buscarPorUsername(request.getUsername());
        log.debug("Login: usuario '{}' encontrado={}", request.getUsername(), usuario.isPresent());

        if (usuario.isEmpty()) {
            log.warn("Intento de login fallido para username '{}'", request.getUsername());
            return Optional.empty();
        }

        Usuario encontrado = usuario.get();
        log.debug("Login: usuario '{}' activo={}", encontrado.getUsername(), encontrado.isActivo());

        boolean passwordCoincide = encontrado.getPassword().equals(request.getPassword());
        log.debug("Login: password recibida para '{}' coincide={}", encontrado.getUsername(), passwordCoincide);

        if (!encontrado.isActivo() || !passwordCoincide) {
            log.warn("Intento de login fallido para username '{}'", request.getUsername());
            return Optional.empty();
        }

        String token = jwtService.generarToken(encontrado.getUsername(), encontrado.getRol());
        log.debug("Login: JWT generado correctamente para '{}' (rol {})", encontrado.getUsername(), encontrado.getRol());

        return Optional.of(LoginResponseDTO.builder()
                .success(true)
                .message(MENSAJE_LOGIN_EXITOSO)
                .token(token)
                .username(encontrado.getUsername())
                .role(encontrado.getRol().name())
                .build());
    }
}
