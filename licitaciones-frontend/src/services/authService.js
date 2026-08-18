import * as authApi from "../api/authApi";

/**
 * Capa de servicio de autenticacion: envuelve api/authApi.js devolviendo
 * datos ya desempaquetados y errores normalizados. A diferencia de
 * services/licitacionService.js (que usa utils/errorHandler.js), el login
 * fallido tiene su PROPIO contrato de error ({success:false, message},
 * ver AuthController) distinto al ErrorResponseDTO del resto de la API,
 * asi que se normaliza aparte, aqui mismo.
 */

const MENSAJE_SIN_CONEXION =
  "No fue posible conectar con el servidor. Verifica que el backend esté disponible.";
const MENSAJE_GENERICO = "Ocurrió un error inesperado. Intenta nuevamente.";

/**
 * Inicia sesion.
 * @param {string} username
 * @param {string} password
 * @returns {Promise<{success: true, token: string, username: string, role: string}>}
 * @throws {{status: number, message: string}}
 */
export async function login(username, password) {
  try {
    const { data } = await authApi.login(username, password);
    return data;
  } catch (error) {
    throw parseLoginError(error);
  }
}

/**
 * @param {import("axios").AxiosError} error
 * @returns {{status: number, message: string}}
 */
function parseLoginError(error) {
  if (!error?.response) {
    return { status: 0, message: MENSAJE_SIN_CONEXION };
  }
  const mensaje = error.response.data?.message;
  return { status: error.response.status, message: mensaje ?? MENSAJE_GENERICO };
}
