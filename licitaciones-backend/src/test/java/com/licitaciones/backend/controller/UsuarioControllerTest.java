package com.licitaciones.backend.controller;

import com.licitaciones.backend.dto.request.CrearUsuarioRequestDTO;
import com.licitaciones.backend.dto.response.LoginErrorResponseDTO;
import com.licitaciones.backend.dto.response.UsuarioResponseDTO;
import com.licitaciones.backend.entity.Rol;
import com.licitaciones.backend.entity.Usuario;
import com.licitaciones.backend.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Contrato exacto de POST /api/usuarios pedido en la seccion 6/8: 201 con el
 * usuario creado (sin password) si el username no existia, 409 con
 * {success:false, message:"El usuario ya existe"} si ya existia. La
 * restriccion de que solo ADMIN puede llegar aqui la aplica Spring Security
 * (hasRole("ADMIN") en security.SecurityConfig, ver
 * security.SecurityConfigIntegrationTest), no este controller.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;

    private final CrearUsuarioRequestDTO request = new CrearUsuarioRequestDTO("TatianaSofgic", "LicitacionesTati#2026", Rol.USER);

    @Test
    void usernameNuevo_devuelve201ConElUsuarioCreadoSinPassword() {
        UsuarioController controller = new UsuarioController(usuarioService);
        Usuario creado = Usuario.builder().id(5L).username("TatianaSofgic").password("LicitacionesTati#2026").rol(Rol.USER).activo(true).build();
        when(usuarioService.crearUsuario("TatianaSofgic", "LicitacionesTati#2026", Rol.USER)).thenReturn(Optional.of(creado));

        ResponseEntity<?> respuesta = controller.crear(request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isInstanceOf(UsuarioResponseDTO.class);
        UsuarioResponseDTO body = (UsuarioResponseDTO) respuesta.getBody();
        assertThat(body.getId()).isEqualTo(5L);
        assertThat(body.getUsername()).isEqualTo("TatianaSofgic");
        assertThat(body.getRol()).isEqualTo("USER");
        assertThat(body.isActivo()).isTrue();
    }

    @Test
    void usernameYaExistente_devuelve409ConMensajeExacto() {
        UsuarioController controller = new UsuarioController(usuarioService);
        when(usuarioService.crearUsuario("TatianaSofgic", "LicitacionesTati#2026", Rol.USER)).thenReturn(Optional.empty());

        ResponseEntity<?> respuesta = controller.crear(request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody()).isInstanceOf(LoginErrorResponseDTO.class);
        LoginErrorResponseDTO error = (LoginErrorResponseDTO) respuesta.getBody();
        assertThat(error.isSuccess()).isFalse();
        assertThat(error.getMessage()).isEqualTo("El usuario ya existe");
    }
}
