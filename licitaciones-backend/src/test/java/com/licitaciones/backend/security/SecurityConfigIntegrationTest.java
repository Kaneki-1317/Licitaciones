package com.licitaciones.backend.security;

import com.licitaciones.backend.auth.JwtService;
import com.licitaciones.backend.config.JwtProperties;
import com.licitaciones.backend.entity.Rol;
import com.licitaciones.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de extremo a extremo (seccion 21 del requerimiento) contra la
 * cadena REAL de Spring Security: SecurityConfig + JwtAuthenticationFilter
 * + JwtAuthenticationEntryPoint, tal como quedan cableados en la
 * aplicacion.
 *
 * Usa GET /api/health/version (HealthController, ya existente) como
 * "endpoint protegido" de prueba porque no toca la base de datos: valida
 * la proteccion generica de la seccion 6 ("todo lo demas requiere
 * autenticacion") sin escribir nada en MySQL. POST /api/auth/login se
 * prueba aqui solo para confirmar que sigue siendo publico incluso con
 * todo el filtro de seguridad activo (la logica de AuthService/AuthController
 * ya esta cubierta por sus propios tests unitarios).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private static final String USERNAME_PRUEBA = "usuario-de-prueba-security-config-integration-test";

    /** Limpia cualquier usuario que el test de creacion haya llegado a insertar en la BD real. */
    @AfterEach
    void limpiarUsuarioDePrueba() {
        usuarioRepository.findByUsername(USERNAME_PRUEBA).ifPresent(usuarioRepository::delete);
    }

    @Test
    void endpointProtegido_sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/health/version"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void endpointProtegido_conTokenValidoDevuelve200() throws Exception {
        String token = jwtService.generarToken("admin", Rol.ADMIN);

        mockMvc.perform(get("/api/health/version").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void endpointProtegido_conTokenInvalidoDevuelve401() throws Exception {
        mockMvc.perform(get("/api/health/version").header("Authorization", "Bearer token-invalido-o-manipulado"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointProtegido_conTokenExpiradoDevuelve401() throws Exception {
        // Mismo secreto configurado en la app (JwtProperties real), solo con
        // expiracion negativa: aisla "expirado" como unica variable (si
        // usara otro secreto, fallaria por firma invalida, no por expiracion).
        JwtService servicioYaVencido = new JwtService(new JwtProperties(jwtProperties.secret(), -1_000));
        String tokenExpirado = servicioYaVencido.generarToken("admin", Rol.ADMIN);

        mockMvc.perform(get("/api/health/version").header("Authorization", "Bearer " + tokenExpirado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSigueSiendoPublico_aunConTodaLaSeguridadActiva() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"no-existe\",\"password\":\"lo-que-sea\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    /**
     * Seccion 6/9 del requerimiento: crear usuarios exige rol ADMIN. Prueba
     * de extremo a extremo contra la cadena REAL (SecurityConfig +
     * hasRole("ADMIN") + JwtAccessDeniedHandler), sin necesidad de que exista
     * un usuario USER real en la BD: el filtro construye la autoridad
     * directamente desde el claim "role" del JWT.
     */
    @Test
    void crearUsuario_sinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"x\",\"password\":\"x\",\"rol\":\"USER\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearUsuario_conTokenDeRolUSER_devuelve403() throws Exception {
        String tokenUser = jwtService.generarToken("un-user-cualquiera", Rol.USER);

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + tokenUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"x\",\"password\":\"x\",\"rol\":\"USER\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void crearUsuario_conTokenDeRolADMIN_devuelve201() throws Exception {
        String tokenAdmin = jwtService.generarToken("un-admin-cualquiera", Rol.ADMIN);

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + USERNAME_PRUEBA + "\",\"password\":\"claveDePrueba\",\"rol\":\"USER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(USERNAME_PRUEBA))
                .andExpect(jsonPath("$.rol").value("USER"))
                .andExpect(jsonPath("$.activo").value(true));

        // La contrasena quedo EXACTAMENTE como se envio, sin hashear.
        assertThat(usuarioRepository.findByUsername(USERNAME_PRUEBA))
                .isPresent()
                .get()
                .extracting(u -> u.getPassword())
                .isEqualTo("claveDePrueba");
    }
}
