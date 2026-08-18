import * as usuariosApi from "../api/usuariosApi";

/**
 * Capa de servicio para la creacion de usuarios (solo ADMIN). Normaliza el
 * error al MISMO contrato que ya usa el resto de la app
 * ({status, error, mensaje, detalles[]}, ver utils/errorHandler.js y
 * ErrorAlert), aunque el backend responda con dos formas distintas segun el
 * caso:
 *   - 409 (username duplicado): {success:false, message:"El usuario ya existe"}
 *   - 400 (validacion) / 403 (rol insuficiente): {mensaje, error, detalles[]}
 *     (el contrato ErrorResponseDTO general del backend)
 * Por eso NO se reutiliza parseApiError tal cual (que solo entiende la
 * segunda forma): esta funcion lee "message" primero y cae a "mensaje" si
 * no esta.
 */

/**
 * Crea un usuario nuevo.
 * @param {string} username
 * @param {string} password
 * @param {"ADMIN"|"USER"} rol
 * @returns {Promise<object>} UsuarioResponseDTO
 * @throws {{status: number, error?: string, mensaje: string, detalles: string[]}}
 */
export async function crearUsuario(username, password, rol) {
  try {
    const { data } = await usuariosApi.crearUsuario(username, password, rol);
    return data;
  } catch (error) {
    throw await parseUsuarioError(error);
  }
}

async function parseUsuarioError(error) {
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

  const data = error.response.data;

  return {
    status: error.response.status,
    error: data?.error ?? error.response.statusText ?? "Error",
    mensaje: data?.message ?? data?.mensaje ?? "Ocurrio un error inesperado al crear el usuario.",
    detalles: Array.isArray(data?.detalles) ? data.detalles : [],
  };
}
