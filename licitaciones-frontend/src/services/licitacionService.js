import * as licitacionesApi from "../api/licitacionesApi";
import { parseApiError } from "../utils/errorHandler";
import { triggerBlobDownload } from "../utils/downloadFile";
import { EXCEL_FILENAME } from "../utils/constants";

/**
 * Capa de servicio: envuelve la API cruda (api/licitacionesApi.js) para
 * devolver siempre datos ya desempaquetados (response.data) y errores
 * normalizados al contrato ErrorResponseDTO (ver utils/errorHandler.js).
 * Las paginas y hooks solo deberian hablar con este modulo, nunca con
 * axios directamente.
 */

/**
 * Crea un nuevo proceso de licitacion.
 * @param {string} nombreProceso
 * @returns {Promise<object>} ProcesoResponseDTO
 */
export async function crearProceso(nombreProceso) {
  try {
    const { data } = await licitacionesApi.crearProceso(nombreProceso);
    return data;
  } catch (error) {
    throw await parseApiError(error);
  }
}

/**
 * Lista el historial completo de procesos.
 * @returns {Promise<object[]>} ProcesoResponseDTO[]
 */
export async function listarProcesos() {
  try {
    const { data } = await licitacionesApi.listarProcesos();
    return data;
  } catch (error) {
    throw await parseApiError(error);
  }
}

/**
 * Consulta el detalle de un proceso puntual.
 * @param {number|string} id
 * @returns {Promise<object>} ProcesoResponseDTO
 */
export async function obtenerProceso(id) {
  try {
    const { data } = await licitacionesApi.obtenerProceso(id);
    return data;
  } catch (error) {
    throw await parseApiError(error);
  }
}

/**
 * Envia los documentos seleccionados a analisis. Esta llamada puede tardar
 * varios minutos: el backend espera de forma sincrona la respuesta de n8n
 * y genera el Excel antes de responder.
 * @param {number|string} id
 * @param {File[]} archivos
 * @returns {Promise<object>} ProcesoResponseDTO (estado COMPLETADO si tuvo exito)
 */
export async function analizarDocumentos(id, archivos) {
  try {
    const { data } = await licitacionesApi.analizarDocumentos(id, archivos);
    return data;
  } catch (error) {
    throw await parseApiError(error);
  }
}

/**
 * Consulta el estado actual del proceso.
 * @param {number|string} id
 * @returns {Promise<{estado: string}>}
 */
export async function consultarEstado(id) {
  try {
    const { data } = await licitacionesApi.consultarEstado(id);
    return data;
  } catch (error) {
    throw await parseApiError(error);
  }
}

/**
 * Consulta el resultado del ultimo analisis: { success, data: { fichaTecnica } }.
 * Hoy el backend solo tiene implementado el modulo de Ficha Tecnica. Lanza
 * un error normalizado con status 409 si el proceso aun no ha sido
 * analizado.
 * @param {number|string} id
 * @returns {Promise<object>}
 */
export async function consultarResultado(id) {
  try {
    const { data } = await licitacionesApi.consultarResultado(id);
    return data;
  } catch (error) {
    throw await parseApiError(error);
  }
}

/**
 * Descarga el Excel final del proceso y dispara el guardado en el
 * navegador con el nombre real del archivo.
 * @param {number|string} id
 */
export async function descargarExcel(id) {
  try {
    const { data } = await licitacionesApi.descargarExcel(id);
    triggerBlobDownload(data, EXCEL_FILENAME);
  } catch (error) {
    throw await parseApiError(error);
  }
}
