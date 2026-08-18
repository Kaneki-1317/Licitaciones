package com.licitaciones.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.licitaciones.backend.dto.response.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Responde 403 (mismo contrato {@link ErrorResponseDTO} que
 * {@link JwtAuthenticationEntryPoint} usa para el 401) cuando el JWT es
 * valido pero el usuario NO tiene el rol requerido — hoy, el unico caso es
 * un usuario con rol distinto de ADMIN llamando POST /api/usuarios (ver
 * {@code security.SecurityConfig}: {@code hasRole("ADMIN")}).
 *
 * A diferencia del 401 (no autenticado), aqui la autenticacion SI fue
 * valida; simplemente no alcanza para esta operacion.
 */
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        ErrorResponseDTO body = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .mensaje("No tienes permisos para realizar esta accion.")
                .path(request.getRequestURI())
                .detalles(List.of())
                .build();

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
