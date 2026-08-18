package com.licitaciones.backend.security;

import com.licitaciones.backend.auth.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuracion central de Spring Security: la aplicacion es privada por
 * completo salvo POST /api/auth/login (unico endpoint publico).
 *
 * No hay sesion de servidor (JWT es stateless: cada request se autentica
 * de nuevo via {@link JwtAuthenticationFilter}), por eso CSRF esta
 * deshabilitado (no aplica: el token viaja en el header Authorization, no
 * en una cookie) y formLogin/httpBasic tampoco se habilitan (unico login
 * es POST /api/auth/login).
 *
 * Sin PasswordEncoder: las contrasenas se comparan en texto plano (ver
 * auth.AuthService), decision explicita del proyecto. Ya no hay bean
 * BCryptPasswordEncoder aqui ni en ningun otro lado del backend.
 *
 * POST /api/usuarios (crear usuario, ver controller.UsuarioController) exige
 * ademas el rol ADMIN: es el unico endpoint con una regla de autorizacion
 * mas especifica que "estar autenticado".
 *
 * CORS: se reutiliza la configuracion ya existente para el frontend Vite
 * ({@code config.CorsConfig}, registrada como WebMvcConfigurer). Al no
 * declarar aqui un bean CorsConfigurationSource propio, Spring Security
 * delega automaticamente en esa misma configuracion de Spring MVC (ver
 * https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html),
 * sin duplicar el origen permitido.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
