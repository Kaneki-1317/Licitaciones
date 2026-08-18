package com.licitaciones.backend.auth;

import com.licitaciones.backend.dto.request.LoginRequestDTO;
import com.licitaciones.backend.dto.response.LoginErrorResponseDTO;
import com.licitaciones.backend.dto.response.LoginResponseDTO;
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
 * Contrato exacto de POST /api/auth/login pedido en la seccion 4: 200 con
 * {success:true, token, username, role} si AuthService valida las
 * credenciales, 401 con {success:false, message:"Credenciales inválidas"}
 * si no (sin importar la causa).
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private final LoginRequestDTO request = new LoginRequestDTO("admin", "clave");

    @Test
    void loginExitoso_devuelve200ConElBodyDeAuthService() {
        AuthController controller = new AuthController(authService);
        LoginResponseDTO body = LoginResponseDTO.builder()
                .success(true).token("un.jwt.valido").username("admin").role("ADMIN").build();
        when(authService.login(request)).thenReturn(Optional.of(body));

        ResponseEntity<?> respuesta = controller.login(request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isSameAs(body);
    }

    @Test
    void loginFallido_devuelve401ConMensajeGenerico() {
        AuthController controller = new AuthController(authService);
        when(authService.login(request)).thenReturn(Optional.empty());

        ResponseEntity<?> respuesta = controller.login(request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getBody()).isInstanceOf(LoginErrorResponseDTO.class);
        LoginErrorResponseDTO error = (LoginErrorResponseDTO) respuesta.getBody();
        assertThat(error.isSuccess()).isFalse();
        assertThat(error.getMessage()).isEqualTo("Credenciales inválidas");
    }
}
