package com.licitaciones.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.licitaciones.backend.dto.response.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Responde 401 (con el mismo contrato {@link ErrorResponseDTO} que usa
 * {@code GlobalExceptionHandler} para el resto de la API, ver
 * frontend utils/errorHandler.js) cuando se intenta acceder a un recurso
 * protegido sin un JWT valido: header ausente, token expirado, mal
 * formado, o con firma invalida. {@link com.licitaciones.backend.auth.JwtAuthenticationFilter}
 * nunca lanza excepcion; es la falta de autenticacion en el
 * SecurityContext lo que hace que Spring Security invoque este entry
 * point.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        ErrorResponseDTO body = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .mensaje("No autenticado o sesión expirada. Inicia sesión nuevamente.")
                .path(request.getRequestURI())
                .detalles(List.of())
                .build();

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
