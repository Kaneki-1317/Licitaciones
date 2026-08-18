import axios from "axios";
import { leerSesion, limpiarSesion } from "../utils/authStorage";

/**
 * URL base del backend Spring Boot. Se toma de la variable de entorno
 * VITE_API_BASE_URL (ver .env.example); si no esta definida, cae al valor
 * de desarrollo local usado por el backend (server.port=8080).
 */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

const LOGIN_PATH = "/api/auth/login";

/**
 * Instancia de Axios compartida por toda la aplicacion. Centraliza la
 * autenticacion en dos interceptores (ver seccion 11/12 del pedido) para
 * no tener que tocar cada llamada de api/*.js una por una:
 *
 * - request: agrega "Authorization: Bearer <token>" a toda peticion, si
 *   hay sesion guardada (ver utils/authStorage.js).
 * - response: si el backend responde 401 en una peticion que NO era el
 *   login (un login fallido tambien devuelve 401, pero ese lo maneja la
 *   pagina de Login con su propio mensaje, no es una sesion expirada),
 *   limpia la sesion y manda al usuario a /login para que vuelva a
 *   autenticarse, en vez de dejarlo en una pantalla que siga intentando
 *   peticiones que van a seguir fallando.
 */
const axiosClient = axios.create({
  baseURL: API_BASE_URL,
});

axiosClient.interceptors.request.use((config) => {
  const sesion = leerSesion();
  if (sesion?.token) {
    config.headers.Authorization = `Bearer ${sesion.token}`;
  }
  return config;
});

axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const esLogin = error.config?.url?.includes(LOGIN_PATH);
    const enPaginaDeLogin = window.location.pathname === "/login";

    if (error.response?.status === 401 && !esLogin && !enPaginaDeLogin) {
      limpiarSesion();
      // Redireccion dura (no useNavigate): este interceptor corre fuera del
      // arbol de React, y una recarga completa garantiza que ningun estado
      // ni peticion en curso de la sesion vieja quede vivo.
      window.location.assign("/login?sessionExpired=1");
    }
    return Promise.reject(error);
  },
);

export default axiosClient;
