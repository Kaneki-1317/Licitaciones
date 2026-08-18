package com.licitaciones.backend.auth;

import com.licitaciones.backend.dto.request.LoginRequestDTO;
import com.licitaciones.backend.dto.response.LoginResponseDTO;
import com.licitaciones.backend.entity.Rol;
import com.licitaciones.backend.entity.Usuario;
import com.licitaciones.backend.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre la seccion 1 del requerimiento (comparacion DIRECTA de password, sin
 * BCrypt): login correcto devuelve el JWT ({success:true, message, token,
 * username, role}), y usuario inexistente, contrasena incorrecta y usuario
 * inactivo devuelven TODOS el mismo resultado vacio (el controller los
 * traduce, todos por igual, al mismo mensaje generico "Credenciales
 * inválidas") sin filtrar cual de los tres ocurrio.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioService usuarioService;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    private final LoginRequestDTO request = new LoginRequestDTO("admin", "clave-correcta");

    @BeforeEach
    void crearServicio() {
        authService = new AuthService(usuarioService, jwtService);
    }

    @Test
    void credencialesCorrectas_devuelveTokenConUsernameYRol() {
        Usuario admin = Usuario.builder().username("admin").password("clave-correcta").rol(Rol.ADMIN).activo(true).build();
        when(usuarioService.buscarPorUsername("admin")).thenReturn(Optional.of(admin));
        when(jwtService.generarToken("admin", Rol.ADMIN)).thenReturn("un.jwt.valido");

        Optional<LoginResponseDTO> resultado = authService.login(request);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().isSuccess()).isTrue();
        assertThat(resultado.get().getMessage()).isEqualTo("Login exitoso");
        assertThat(resultado.get().getToken()).isEqualTo("un.jwt.valido");
        assertThat(resultado.get().getUsername()).isEqualTo("admin");
        assertThat(resultado.get().getRole()).isEqualTo("ADMIN");
    }

    @Test
    void credencialesCorrectas_deUnUsuarioConRolUSER_tambienFunciona() {
        Usuario usuario = Usuario.builder().username("admin").password("clave-correcta").rol(Rol.USER).activo(true).build();
        when(usuarioService.buscarPorUsername("admin")).thenReturn(Optional.of(usuario));
        when(jwtService.generarToken("admin", Rol.USER)).thenReturn("otro.jwt");

        Optional<LoginResponseDTO> resultado = authService.login(request);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getRole()).isEqualTo("USER");
    }

    @Test
    void usuarioInexistente_devuelveVacio() {
        when(usuarioService.buscarPorUsername("admin")).thenReturn(Optional.empty());

        assertThat(authService.login(request)).isEmpty();
    }

    @Test
    void passwordIncorrecta_devuelveVacio() {
        Usuario admin = Usuario.builder().username("admin").password("otra-clave-distinta").rol(Rol.ADMIN).activo(true).build();
        when(usuarioService.buscarPorUsername("admin")).thenReturn(Optional.of(admin));

        assertThat(authService.login(request)).isEmpty();
    }

    @Test
    void usuarioInactivo_devuelveVacioAunqueLaPasswordCoincida() {
        Usuario inactivo = Usuario.builder().username("admin").password("clave-correcta").rol(Rol.ADMIN).activo(false).build();
        when(usuarioService.buscarPorUsername("admin")).thenReturn(Optional.of(inactivo));

        assertThat(authService.login(request)).isEmpty();
    }

    @Test
    void nuncaGeneraTokenSiLasCredencialesSonInvalidas() {
        when(usuarioService.buscarPorUsername("admin")).thenReturn(Optional.empty());

        authService.login(request);

        verify(jwtService, never()).generarToken(any(), any());
    }
}
