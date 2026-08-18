package com.licitaciones.backend.auth;

import com.licitaciones.backend.config.JwtProperties;
import com.licitaciones.backend.entity.Rol;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

/**
 * Unico responsable de emitir y validar los JWT de la aplicacion.
 *
 * El token solo lleva lo indispensable para reconstruir la autenticacion
 * SIN volver a consultar la base de datos en cada request (ver
 * {@link JwtAuthenticationFilter}): "sub" (username), el claim "role" y las
 * fechas estandar "iat"/"exp". Nunca se incluye la contrasena ni ningun
 * otro dato sensible.
 */
@Service
@Slf4j
public class JwtService {

    private static final String CLAIM_ROL = "role";

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(JwtProperties jwtProperties) {
        this.clave = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.expiracionMs = jwtProperties.expirationMs();
    }

    /** Genera un JWT firmado para el usuario autenticado. */
    public String generarToken(String username, Rol rol) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expiracionMs);

        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_ROL, rol.name())
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(clave)
                .compact();
    }

    /**
     * Valida la firma y expiracion del token y, si es valido, devuelve sus
     * claims (username + rol). Vacio si el token es invalido, esta mal
     * formado, fue alterado o ya expiro: nunca lanza hacia el llamador, para
     * que el filtro simplemente trate cualquier fallo como "no autenticado".
     */
    public Optional<TokenClaims> validarYExtraer(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.getSubject();
            String rol = claims.get(CLAIM_ROL, String.class);
            if (username == null || rol == null) {
                return Optional.empty();
            }
            return Optional.of(new TokenClaims(username, Rol.valueOf(rol)));
        } catch (JwtException | IllegalArgumentException ex) {
            // Firma invalida, token expirado, formato corrupto, rol desconocido, etc.
            // No se registra el token en el log (evita filtrar credenciales/JWT).
            log.debug("JWT invalido o expirado: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /** Username y rol ya validados, extraidos del JWT. */
    public record TokenClaims(String username, Rol rol) {
    }
}
