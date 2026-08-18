package com.licitaciones.backend.auth;

import com.licitaciones.backend.entity.Rol;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre la seccion 7 del requerimiento: el filtro lee "Authorization:
 * Bearer <token>", y SOLO si JwtService lo valida carga la autenticacion
 * en el SecurityContext (username + authority ROLE_<rol>, sin volver a
 * consultar la base de datos). Si el header falta o el token es invalido,
 * no autentica y deja que SecurityConfig/JwtAuthenticationEntryPoint
 * respondan 401 mas adelante en la cadena.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tokenValido_autenticaConUsernameYRolDelToken() throws Exception {
        JwtAuthenticationFilter filtroConMock = new JwtAuthenticationFilter(jwtService);
        when(request.getHeader("Authorization")).thenReturn("Bearer un.jwt.valido");
        when(jwtService.validarYExtraer("un.jwt.valido"))
                .thenReturn(Optional.of(new JwtService.TokenClaims("admin", Rol.ADMIN)));

        filtroConMock.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("admin");
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void tokenValido_conRolUSER_autenticaConRoleUser() throws Exception {
        JwtAuthenticationFilter filtroConMock = new JwtAuthenticationFilter(jwtService);
        when(request.getHeader("Authorization")).thenReturn("Bearer un.jwt.de.user");
        when(jwtService.validarYExtraer("un.jwt.de.user"))
                .thenReturn(Optional.of(new JwtService.TokenClaims("TatianaSofgic", Rol.USER)));

        filtroConMock.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("TatianaSofgic");
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER");
    }

    @Test
    void sinHeaderAuthorization_noAutentica() throws Exception {
        JwtAuthenticationFilter filtroConMock = new JwtAuthenticationFilter(jwtService);
        when(request.getHeader("Authorization")).thenReturn(null);

        filtroConMock.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void tokenInvalido_noAutenticaYContinuaLaCadena() throws Exception {
        JwtAuthenticationFilter filtroConMock = new JwtAuthenticationFilter(jwtService);
        when(request.getHeader("Authorization")).thenReturn("Bearer token-invalido");
        when(jwtService.validarYExtraer("token-invalido")).thenReturn(Optional.empty());

        filtroConMock.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void headerSinPrefijoBearer_seIgnora() throws Exception {
        JwtAuthenticationFilter filtroConMock = new JwtAuthenticationFilter(jwtService);
        when(request.getHeader("Authorization")).thenReturn("un.jwt.valido");

        filtroConMock.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
