package com.licitaciones.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de entrada de la aplicacion.
 *
 * Backend de automatizacion de licitaciones: expone la API REST consumida
 * por el frontend React, orquesta el analisis de documentos delegado a n8n
 * y genera el Excel final a partir de la plantilla oficial.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LicitacionesBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(LicitacionesBackendApplication.class, args);
	}

}
