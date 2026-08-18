import { useCallback, useMemo, useState } from "react";
import { AuthContext } from "./authContextObject";
import * as authService from "../services/authService";
import { guardarSesion, leerSesion, limpiarSesion } from "../utils/authStorage";

/**
 * Mecanismo centralizado de sesion (ver seccion 9 del pedido): expone
 * login()/logout(), el usuario autenticado y el token, para que
 * ProtectedRoute y cualquier pagina/componente puedan consultarlos sin
 * volver a hablar con localStorage directamente (eso lo hace
 * utils/authStorage.js). El hook para consumirlo (useAuth) vive aparte en
 * hooks/useAuth.js, y el objeto de contexto en si en authContextObject.js:
 * mismo criterio que el resto de la app (hooks/useProcesos.js), y evita
 * mezclar un componente con un export que no lo es en el mismo archivo
 * (rompe React Fast Refresh).
 *
 * La sesion se restaura de forma SINCRONA leyendo localStorage en el
 * useState inicial: no hay JWT a decodificar ni llamada de red que esperar
 * (username/role ya vienen guardados desde el login, ver LoginResponseDTO
 * en el backend), asi que no hace falta un estado de "loading" para
 * ProtectedRoute ni pantallazos en blanco al recargar la pagina.
 */

export function AuthProvider({ children }) {
  const [sesion, setSesion] = useState(() => leerSesion());

  const login = useCallback(async (username, password) => {
    const data = await authService.login(username, password);
    const nuevaSesion = { token: data.token, username: data.username, role: data.role };
    guardarSesion(nuevaSesion);
    setSesion(nuevaSesion);
    return nuevaSesion;
  }, []);

  const logout = useCallback(() => {
    limpiarSesion();
    setSesion(null);
  }, []);

  const value = useMemo(
    () => ({
      user: sesion ? { username: sesion.username, role: sesion.role } : null,
      token: sesion?.token ?? null,
      isAuthenticated: Boolean(sesion?.token),
      login,
      logout,
    }),
    [sesion, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
