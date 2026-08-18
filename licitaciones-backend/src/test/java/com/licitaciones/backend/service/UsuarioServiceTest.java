package com.licitaciones.backend.service;

import com.licitaciones.backend.entity.Rol;
import com.licitaciones.backend.entity.Usuario;
import com.licitaciones.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre las secciones 7/8 del requerimiento: crearUsuario guarda la
 * contrasena EXACTAMENTE como llega (sin BCrypt ni ningun otro hashing),
 * activo siempre en true, y username duplicado devuelve vacio (sin llegar
 * a guardar nada) en vez de lanzar una excepcion.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Captor
    private ArgumentCaptor<Usuario> usuarioCapturado;

    private UsuarioService servicio() {
        return new UsuarioService(usuarioRepository);
    }

    @Test
    void crearUsuario_guardaLaPasswordTalCualSinHashearla() {
        when(usuarioRepository.existsByUsername("TatianaSofgic")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        Optional<Usuario> resultado = servicio().crearUsuario("TatianaSofgic", "LicitacionesTati#2026", Rol.USER);

        assertThat(resultado).isPresent();
        verify(usuarioRepository).save(usuarioCapturado.capture());
        Usuario guardado = usuarioCapturado.getValue();
        assertThat(guardado.getUsername()).isEqualTo("TatianaSofgic");
        assertThat(guardado.getPassword()).isEqualTo("LicitacionesTati#2026");
        assertThat(guardado.getRol()).isEqualTo(Rol.USER);
        assertThat(guardado.isActivo()).isTrue();
    }

    @Test
    void crearUsuario_conUsernameYaExistente_devuelveVacioSinGuardarNada() {
        when(usuarioRepository.existsByUsername("admin")).thenReturn(true);

        Optional<Usuario> resultado = servicio().crearUsuario("admin", "cualquierClave", Rol.ADMIN);

        assertThat(resultado).isEmpty();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void crearAdminSiNoExiste_guardaLaPasswordTalCualSinHashearla() {
        when(usuarioRepository.existsByUsername("admin")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        servicio().crearAdminSiNoExiste("admin", "Softgic2021");

        verify(usuarioRepository).save(usuarioCapturado.capture());
        Usuario guardado = usuarioCapturado.getValue();
        assertThat(guardado.getUsername()).isEqualTo("admin");
        assertThat(guardado.getPassword()).isEqualTo("Softgic2021");
        assertThat(guardado.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(guardado.isActivo()).isTrue();
    }

    @Test
    void crearAdminSiNoExiste_noTocaElUsuarioSiYaExiste() {
        when(usuarioRepository.existsByUsername("admin")).thenReturn(true);

        servicio().crearAdminSiNoExiste("admin", "loQueSeaQueLlegue");

        verify(usuarioRepository, never()).save(any());
    }
}
