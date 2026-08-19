package com.licitaciones.backend.config;

import com.licitaciones.backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Mecanismo de inicializacion del primer usuario administrador (los demas
 * usuarios, incluyendo otros ADMIN, se crean con POST /api/usuarios, ver
 * controller.UsuarioController): no existe un endpoint de registro publico.
 * Al arrancar la aplicacion, si llegan ADMIN_USERNAME/ADMIN_PASSWORD (ver
 * {@link AdminInitProperties} / app.admin.* en application.properties) crea
 * ese usuario con la contrasena tal cual (texto plano, ver UsuarioService),
 * y no hace nada si ya existe.
 *
 * Si ADMIN_PASSWORD no se define, NO se crea ningun usuario: es preferible
 * arrancar sin admin (y dejarlo explicito en el log) a hardcodear una
 * contrasena por defecto en el codigo fuente.
 *
 * IMPORTANTE: este componente es OPCIONAL y NO condiciona el login en
 * absoluto. {@code auth.AuthService} valida credenciales consultando
 * directamente {@code UsuarioRepository.findByUsername(...)} contra la
 * tabla "usuarios" -- no depende de que este initializer haya corrido, ni
 * de ADMIN_USERNAME/ADMIN_PASSWORD, ni de ningun estado que este
 * ApplicationRunner deje. Su unico proposito es crear el usuario cuando
 * la tabla esta vacia (primer arranque contra una base nueva); si ya hay
 * usuarios (como en el Aiven de produccion), no hace falta definir estas
 * variables para nada, y el mensaje de log de abajo es completamente
 * normal, no indica ningun problema.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUsuarioInitializer implements ApplicationRunner {

    private final AdminInitProperties adminInitProperties;
    private final UsuarioService usuarioService;

    @Override
    public void run(ApplicationArguments args) {
        String username = adminInitProperties.username();
        String password = adminInitProperties.password();

        if (!StringUtils.hasText(password)) {
            log.info("ADMIN_PASSWORD no esta definido: no se crea ningun usuario administrador nuevo. "
                    + "Esto es normal y NO afecta el login de usuarios que ya existen en la base de datos "
                    + "(el login valida directamente contra la tabla \"usuarios\", ver auth.AuthService). "
                    + "Definir ADMIN_USERNAME/ADMIN_PASSWORD solo hace falta para crear el primer usuario "
                    + "en una base todavia vacia.");
            return;
        }
        if (!StringUtils.hasText(username)) {
            log.info("ADMIN_USERNAME esta vacio: no se crea ningun usuario administrador nuevo "
                    + "(no afecta el login de usuarios que ya existen).");
            return;
        }

        usuarioService.crearAdminSiNoExiste(username, password);
    }
}
