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
            log.warn("ADMIN_PASSWORD no esta definido: no se crea/actualiza ningun usuario administrador. "
                    + "Define ADMIN_USERNAME y ADMIN_PASSWORD como variables de entorno para inicializar el "
                    + "primer usuario (no hay endpoint de registro).");
            return;
        }
        if (!StringUtils.hasText(username)) {
            log.warn("ADMIN_USERNAME esta vacio: no se crea ningun usuario administrador.");
            return;
        }

        usuarioService.crearAdminSiNoExiste(username, password);
    }
}
