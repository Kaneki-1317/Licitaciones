import axiosClient from "./axiosConfig";

/**
 * Llamadas HTTP crudas a la API de /api/licitaciones. Este modulo no
 * transforma datos ni maneja errores de negocio: eso vive en
 * services/licitacionService.js. Un solo lugar refleja exactamente los
 * endpoints que expone el backend (ver LicitacionController).
 */

const BASE_PATH = "/api/licitaciones";

/**
 * POST /api/licitaciones
 * Crea un proceso de licitacion.
 * @param {string} nombreProceso
 * @returns {Promise<import("axios").AxiosResponse>} ProcesoResponseDTO
 */
export function crearProceso(nombreProceso) {
  return axiosClient.post(BASE_PATH, { nombreProceso });
}

/**
 * GET /api/licitaciones
 * Lista el historial completo de procesos.
 * @returns {Promise<import("axios").AxiosResponse>} ProcesoResponseDTO[]
 */
export function listarProcesos() {
  return axiosClient.get(BASE_PATH);
}

/**
 * GET /api/licitaciones/{id}
 * @param {number|string} id
 * @returns {Promise<import("axios").AxiosResponse>} ProcesoResponseDTO
 */
export function obtenerProceso(id) {
  return axiosClient.get(`${BASE_PATH}/${id}`);
}

/**
 * POST /api/licitaciones/{id}/analizar
 * Envia los documentos del proceso (multipart/form-data, campo
 * "documentos") a n8n. La respuesta puede tardar varios minutos porque el
 * backend espera de forma sincrona el analisis y la generacion del Excel.
 * @param {number|string} id
 * @param {File[]} archivos
 * @returns {Promise<import("axios").AxiosResponse>} ProcesoResponseDTO
 */
export function analizarDocumentos(id, archivos) {
  const formData = new FormData();
  archivos.forEach((archivo) => formData.append("documentos", archivo));

  return axiosClient.post(`${BASE_PATH}/${id}/analizar`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
    // El analisis en n8n puede tardar varios minutos; sin timeout de
    // cliente para no cortar una peticion que el backend si sigue esperando.
    timeout: 0,
  });
}

/**
 * GET /api/licitaciones/{id}/estado
 * @param {number|string} id
 * @returns {Promise<import("axios").AxiosResponse>} { estado }
 */
export function consultarEstado(id) {
  return axiosClient.get(`${BASE_PATH}/${id}/estado`);
}

/**
 * GET /api/licitaciones/{id}/resultado
 * Ultimo resultado de analisis persistido: { success, data: { fichaTecnica } }.
 * Por ahora el backend solo tiene implementado el modulo de Ficha Tecnica;
 * "documentacion" y "fuentes" (trazabilidad) se agregaran dentro de "data"
 * cuando esos modulos existan en n8n. 409 si el proceso aun no ha sido
 * analizado.
 * @param {number|string} id
 * @returns {Promise<import("axios").AxiosResponse>}
 */
export function consultarResultado(id) {
  return axiosClient.get(`${BASE_PATH}/${id}/resultado`);
}

/**
 * GET /api/licitaciones/{id}/excel
 * Descarga el Excel final generado para el proceso como blob binario.
 * @param {number|string} id
 * @returns {Promise<import("axios").AxiosResponse<Blob>>}
 */
export function descargarExcel(id) {
  return axiosClient.get(`${BASE_PATH}/${id}/excel`, {
    responseType: "blob",
  });
}
