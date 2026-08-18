package com.licitaciones.backend.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Lee el header {@code Authorization: Bearer <token>}, valida el JWT con
 * {@link JwtService} y, si es valido, carga la autenticacion en el
 * {@link SecurityContextHolder} para el resto de la cadena de filtros.
 *
 * La autenticacion se reconstruye UNICAMENTE con los claims ya validados
 * del propio token (username + rol): no se hace ninguna consulta a la base
 * de datos por request, tal como pide la seccion 7 del requerimiento.
 *
 * Si el header falta, esta mal formado o el token no es valido, este
 * filtro simplemente NO establece autenticacion y deja continuar la
 * cadena: es {@link security.SecurityConfig} (via
 * {@code authorizeHttpRequests} + {@code JwtAuthenticationEntryPoint})
 * quien decide devolver 401 para las rutas protegidas.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        extraerToken(request)
                .flatMap(jwtService::validarYExtraer)
                .ifPresent(claims -> autenticar(claims, request));

        filterChain.doFilter(request, response);
    }

    private Optional<String> extraerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIJO_BEARER)) {
            return Optional.of(header.substring(PREFIJO_BEARER.length()));
        }
        return Optional.empty();
    }

    private void autenticar(JwtService.TokenClaims claims, HttpServletRequest request) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + claims.rol().name()));

        var authentication = new UsernamePasswordAuthenticationToken(claims.username(), null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
