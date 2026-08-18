package com.licitaciones.backend.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configura el {@link RestTemplate} usado por {@code client.N8nClient} para
 * comunicarse con n8n, con timeouts tomados de {@link N8nProperties}.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, N8nProperties n8nProperties) {
        return builder
                .setConnectTimeout(Duration.ofMillis(n8nProperties.connectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(n8nProperties.readTimeoutMs()))
                .build();
    }
}
