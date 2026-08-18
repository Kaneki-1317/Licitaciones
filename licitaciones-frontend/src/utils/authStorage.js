/**
 * Unico punto de lectura/escritura de la sesion (JWT + datos del usuario)
 * en el navegador. Lo usan tanto api/axiosConfig.js (interceptor, fuera
 * del arbol de React) como context/AuthContext.jsx (estado de React), para
 * no duplicar la forma de la sesion ni las claves de storage en dos
 * lugares.
 *
 * Se guardan tres claves SEPARADAS ("token", "username", "role") en vez de
 * un unico objeto serializado, a pedido explicito del proyecto:
 *   localStorage.setItem("token", ...)
 *   localStorage.setItem("username", ...)
 *   localStorage.setItem("role", ...)
 * El resto de la app (AuthContext, ProtectedRoute, AdminRoute, paginas)
 * nunca lee estas claves directamente: solo hablan con leerSesion()/
 * guardarSesion()/limpiarSesion(), asi que ese detalle de representacion
 * queda encapsulado aqui.
 *
 * ESTRATEGIA DE ALMACENAMIENTO Y SU RIESGO (ver seccion 14 del pedido
 * original de JWT): se usa localStorage, no una cookie httpOnly. Eso
 * significa que el token (y ahora tambien username/role) son legibles por
 * cualquier script que corra en la pagina: si esta app llegara a tener una
 * vulnerabilidad XSS, ese script podria leer la sesion completa de
 * localStorage y robarla. Se elige igual porque el cliente HTTP actual es
 * Axios puro (sin manejo de cookies/CSRF ni backend preparado para setear
 * cookies), y anadir eso habria significado tocar bastante mas superficie
 * de la app existente.
 */

const CLAVE_TOKEN = "token";
const CLAVE_USERNAME = "username";
const CLAVE_ROLE = "role";

/** @returns {{token: string, username: string, role: string} | null} */
export function leerSesion() {
  const token = localStorage.getItem(CLAVE_TOKEN);
  if (!token) {
    return null;
  }
  return {
    token,
    username: localStorage.getItem(CLAVE_USERNAME),
    role: localStorage.getItem(CLAVE_ROLE),
  };
}

/** @param {{token: string, username: string, role: string}} sesion */
export function guardarSesion(sesion) {
  localStorage.setItem(CLAVE_TOKEN, sesion.token);
  localStorage.setItem(CLAVE_USERNAME, sesion.username);
  localStorage.setItem(CLAVE_ROLE, sesion.role);
}

export function limpiarSesion() {
  localStorage.removeItem(CLAVE_TOKEN);
  localStorage.removeItem(CLAVE_USERNAME);
  localStorage.removeItem(CLAVE_ROLE);
}
