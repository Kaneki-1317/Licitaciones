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
     */
    public Optional<LoginResponseDTO> login(LoginRequestDTO request) {
        Optional<Usuario> usuario = usuarioService.buscarPorUsername(request.getUsername());

        if (usuario.isEmpty() || !usuario.get().isActivo()
                || !usuario.get().getPassword().equals(request.getPassword())) {
            log.warn("Intento de login fallido para username '{}'", request.getUsername());
            return Optional.empty();
        }

        Usuario encontrado = usuario.get();
        String token = jwtService.generarToken(encontrado.getUsername(), encontrado.getRol());

        return Optional.of(LoginResponseDTO.builder()
                .success(true)
                .message(MENSAJE_LOGIN_EXITOSO)
                .token(token)
                .username(encontrado.getUsername())
                .role(encontrado.getRol().name())
                .build());
    }
}
