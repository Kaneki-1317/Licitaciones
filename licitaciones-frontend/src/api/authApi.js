import axiosClient from "./axiosConfig";

/**
 * Llamada HTTP cruda al unico endpoint de autenticacion que expone el
 * backend (ver AuthController). No hay /register: la aplicacion no tiene
 * registro publico, el unico usuario es el administrador inicial creado
 * desde el backend (ADMIN_USERNAME/ADMIN_PASSWORD).
 */

const BASE_PATH = "/api/auth";

/**
 * POST /api/auth/login
 * @param {string} username
 * @param {string} password
 * @returns {Promise<import("axios").AxiosResponse>} { success, token, username, role } (200)
 *   o { success: false, message } (401)
 */
export function login(username, password) {
  return axiosClient.post(`${BASE_PATH}/login`, { username, password });
}
