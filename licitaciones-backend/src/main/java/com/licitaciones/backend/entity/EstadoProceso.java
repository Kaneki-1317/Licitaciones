package com.licitaciones.backend.entity;

/**
 * Estados posibles del ciclo de vida de un {@link ProcesoLicitacion}.
 */
public enum EstadoProceso {

    /** El proceso fue creado pero el analisis aun no ha iniciado. */
    CREADO,

    /** El analisis esta en curso: se envio la informacion a n8n. */
    ANALIZANDO,

    /** El analisis finalizo correctamente y el Excel fue generado. */
    COMPLETADO,

    /** Ocurrio un error durante el analisis o la generacion del Excel. */
    ERROR
}
