package com.licitaciones.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Representa un proceso de licitacion que atraviesa el flujo:
 * CREADO -> ANALIZANDO -> COMPLETADO (o ERROR).
 */
@Entity
@Table(name = "proceso_licitacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcesoLicitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_proceso", nullable = false, length = 255)
    private String nombreProceso;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoProceso estado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_finalizacion")
    private LocalDateTime fechaFinalizacion;

    /** Ruta/nombre del archivo Excel generado para este proceso. */
    @Column(name = "archivo_generado", length = 500)
    private String archivoGenerado;

    @PrePersist
    protected void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoProceso.CREADO;
        }
    }
}
