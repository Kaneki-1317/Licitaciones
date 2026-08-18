package com.licitaciones.backend.auth;

import com.licitaciones.backend.config.JwtProperties;
import com.licitaciones.backend.entity.Rol;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtService es el unico responsable de emitir/validar los JWT de la
 * aplicacion (ver login en AuthService y validacion en
 * JwtAuthenticationFilter). Este test cubre exactamente los escenarios de
 * la seccion 21 del requerimiento: token valido, token expirado y token
 * invalido/manipulado.
 */
class JwtServiceTest {

    private static final String SECRETO = "prueba-secreto-de-al-menos-256-bits-para-firmar-hs256-1234567890";

    private final JwtService jwtService = new JwtService(new JwtProperties(SECRETO, 60_000));

    @Test
    void generarToken_y_validarYExtraer_devuelveElMismoUsernameYRol() {
        String token = jwtService.generarToken("admin", Rol.ADMIN);

        Optional<JwtService.TokenClaims> claims = jwtService.validarYExtraer(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().username()).isEqualTo("admin");
        assertThat(claims.get().rol()).isEqualTo(Rol.ADMIN);
    }

    @Test
    void elTokenNuncaIncluyeLaPassword() {
        String token = jwtService.generarToken("admin", Rol.ADMIN);

        // El payload del JWT es base64 (no cifrado): confirma explicitamente
        // que ningun dato sensible viaja ahi, mas alla de sub/role/iat/exp.
        String payloadDecodificado = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]),
                StandardCharsets.UTF_8);
        assertThat(payloadDecodificado).doesNotContain("password").doesNotContain("Password");
    }

    @Test
    void tokenExpirado_noSeValida() {
        // JwtService con expiracion "negativa": el token nace ya vencido.
        JwtService servicioExpiracionInmediata = new JwtService(new JwtProperties(SECRETO, -1_000));

        String token = servicioExpiracionInmediata.generarToken("admin", Rol.ADMIN);

        assertThat(jwtService.validarYExtraer(token)).isEmpty();
    }

    @Test
    void tokenFirmadoConOtraClave_noSeValida() {
        SecretKey otraClave = Keys.hmacShaKeyFor("otra-clave-completamente-distinta-de-32-bytes!!".getBytes(StandardCharsets.UTF_8));
        String tokenConFirmaAjena = Jwts.builder()
                .subject("admin")
                .claim("role", "ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otraClave)
                .compact();

        assertThat(jwtService.validarYExtraer(tokenConFirmaAjena)).isEmpty();
    }

    @Test
    void tokenMalFormado_noSeValida() {
        assertThat(jwtService.validarYExtraer("esto-no-es-un-jwt")).isEmpty();
    }
}
