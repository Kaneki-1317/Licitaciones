package com.licitaciones.backend.controller;

import com.licitaciones.backend.dto.request.CrearUsuarioRequestDTO;
import com.licitaciones.backend.dto.response.LoginErrorResponseDTO;
import com.licitaciones.backend.dto.response.UsuarioResponseDTO;
import com.licitaciones.backend.entity.Usuario;
import com.licitaciones.backend.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Creacion de usuarios. NO es un registro publico: este endpoint exige un
 * JWT valido con rol ADMIN (ver {@code security.SecurityConfig},
 * {@code hasRole("ADMIN")} sobre POST /api/usuarios) — un usuario con rol
 * USER recibe 403 (ver {@code security.JwtAccessDeniedHandler}), y sin
 * token, 401. No hay endpoint para listar/editar/borrar usuarios: solo se
 * pidio creacion.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private static final String MENSAJE_USUARIO_YA_EXISTE = "El usuario ya existe";

    private final UsuarioService usuarioService;

    /**
     * POST /api/usuarios
     * 201 + el usuario creado (sin password) si el username no existia
     * todavia; 409 + {success:false, message:"El usuario ya existe"} si ya
     * habia un usuario con ese username.
     */
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody CrearUsuarioRequestDTO request) {
        return usuarioService.crearUsuario(request.getUsername(), request.getPassword(), request.getRol())
                .<ResponseEntity<?>>map(usuario -> ResponseEntity.status(HttpStatus.CREATED).body(aRespuesta(usuario)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new LoginErrorResponseDTO(false, MENSAJE_USUARIO_YA_EXISTE)));
    }

    private UsuarioResponseDTO aRespuesta(Usuario usuario) {
        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .rol(usuario.getRol().name())
                .activo(usuario.isActivo())
                .build();
    }
}
