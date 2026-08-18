package com.licitaciones.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Guarda la trazabilidad del resultado del analisis (JSON crudo) devuelto
 * por n8n para un {@link ProcesoLicitacion}, permitiendo consultar el
 * historial del proceso.
 */
@Entity
@Table(name = "resultado_analisis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoAnalisis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proceso_id", nullable = false)
    private Long procesoId;

    /** JSON completo devuelto por n8n (ficha tecnica, documentacion, fuentes). */
    @Lob
    @Column(name = "json_resultado", nullable = false, columnDefinition = "LONGTEXT")
    private String jsonResultado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
