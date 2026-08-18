import axiosClient from "./axiosConfig";

/**
 * Llamada HTTP cruda a /api/usuarios (creacion de usuarios). Requiere estar
 * autenticado como ADMIN (Axios ya agrega el header Authorization via el
 * interceptor de axiosConfig.js); si quien llama no es ADMIN, el backend
 * responde 403 (ver UsuarioController/SecurityConfig).
 */

const BASE_PATH = "/api/usuarios";

/**
 * POST /api/usuarios
 * @param {string} username
 * @param {string} password
 * @param {"ADMIN"|"USER"} rol
 * @returns {Promise<import("axios").AxiosResponse>} UsuarioResponseDTO (201) o {success:false, message} (409)
 */
export function crearUsuario(username, password, rol) {
  return axiosClient.post(BASE_PATH, { username, password, rol });
}
