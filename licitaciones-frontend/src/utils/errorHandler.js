/**
 * Normaliza cualquier error de Axios al contrato ErrorResponseDTO que
 * devuelve GlobalExceptionHandler en el backend:
 * { timestamp, status, error, mensaje, path, detalles[] }
 *
 * Es async porque las respuestas con responseType: "blob" (descarga de
 * Excel) devuelven el cuerpo de error tambien como Blob, y hay que leerlo
 * como texto antes de poder parsear el JSON.
 *
 * @param {import("axios").AxiosError} error
 * @returns {Promise<{status: number, error: string, mensaje: string, detalles: string[]}>}
 */
export async function parseApiError(error) {
  // Sin respuesta del servidor: backend caido, CORS, timeout, sin red, etc.
  if (!error?.response) {
    return {
      status: 0,
      error: "Sin conexion",
      mensaje:
        "No fue posible conectar con el servidor. Verifica que el backend este disponible en " +
        (error?.config?.baseURL ?? "la URL configurada") +
        ".",
      detalles: [],
    };
  }

  let data = error.response.data;

  if (data instanceof Blob) {
    try {
      const texto = await data.text();
      data = texto ? JSON.parse(texto) : {};
    } catch {
      data = {};
    }
  }

  return {
    status: error.response.status,
    error: data?.error ?? error.response.statusText ?? "Error",
    mensaje: data?.mensaje ?? "Ocurrio un error inesperado en el servidor",
    detalles: Array.isArray(data?.detalles) ? data.detalles : [],
  };
}
