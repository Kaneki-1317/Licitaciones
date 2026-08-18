package com.licitaciones.backend.auth;

import com.licitaciones.backend.dto.request.LoginRequestDTO;
import com.licitaciones.backend.dto.response.LoginErrorResponseDTO;
import com.licitaciones.backend.dto.response.LoginResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unico punto de entrada de autenticacion. NO hay registro publico: el
 * unico usuario posible es el administrador creado por
 * {@code config.AdminUsuarioInitializer} (ver ese paquete). Este es
 * ademas el UNICO endpoint publico de toda la API (ver
 * security.SecurityConfig): todo lo demas exige un JWT valido.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/login
     * Devuelve 200 + {success:true, token, username, role} si las
     * credenciales son correctas, o 401 + {success:false, message} si no
     * lo son. El mensaje es siempre generico: nunca revela si el usuario
     * existe o si fue la contrasena la que fallo.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {
        return authService.login(request)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new LoginErrorResponseDTO(false, AuthService.MENSAJE_CREDENCIALES_INVALIDAS)));
    }
}
