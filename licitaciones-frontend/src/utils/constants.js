/**
 * Estados posibles de un ProcesoLicitacion, tal como los define el backend
 * (entity.EstadoProceso). Deben coincidir exactamente con esos literales.
 */
export const ESTADOS_PROCESO = {
  CREADO: "CREADO",
  ANALIZANDO: "ANALIZANDO",
  COMPLETADO: "COMPLETADO",
  ERROR: "ERROR",
};

/**
 * Metadatos de presentacion (etiqueta y color) para cada estado. Los
 * colores referencian los tokens definidos en index.css
 * (--color-estado-*).
 */
export const ESTADO_CONFIG = {
  [ESTADOS_PROCESO.CREADO]: {
    label: "Creado",
    textClass: "text-slate-600",
    bgClass: "bg-slate-100",
    dotClass: "bg-slate-500",
  },
  [ESTADOS_PROCESO.ANALIZANDO]: {
    label: "Analizando",
    textClass: "text-amber-700",
    bgClass: "bg-amber-100",
    dotClass: "bg-amber-500",
  },
  [ESTADOS_PROCESO.COMPLETADO]: {
    label: "Completado",
    textClass: "text-green-700",
    bgClass: "bg-green-100",
    dotClass: "bg-green-600",
  },
  [ESTADOS_PROCESO.ERROR]: {
    label: "Error",
    textClass: "text-red-700",
    bgClass: "bg-red-100",
    dotClass: "bg-red-600",
  },
};

/**
 * Extensiones de documento tipicas de un proceso de contratacion. Es solo
 * informativo para la UI de carga (no hay validacion de tipo en el
 * backend, que acepta cualquier archivo dentro de los limites de tamano).
 */
export const EXTENSIONES_SUGERIDAS = ["pdf", "doc", "docx", "xls", "xlsx", "zip", "rar"];

/**
 * Nombre fijo del archivo Excel final, tal como lo define el backend
 * (ExcelService.NOMBRE_ARCHIVO_FINAL).
 */
export const EXCEL_FILENAME = "Licitacion_Final.xlsx";
