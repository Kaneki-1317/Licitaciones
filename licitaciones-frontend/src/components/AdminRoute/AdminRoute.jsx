import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";

/**
 * Restringe rutas hijas a usuarios con rol ADMIN (ver seccion 9 del pedido:
 * "Usuarios" / /usuarios). Se usa DENTRO de <ProtectedRoute /> (ver
 * App.jsx), asi que aqui siempre hay sesion; solo falta comprobar el rol.
 *
 * Esto es conveniencia de UI, NO la seguridad real: un usuario con rol USER
 * que llame POST /api/usuarios directamente (sin pasar por esta pantalla)
 * recibe 403 del backend igual (ver security.SecurityConfig,
 * hasRole("ADMIN")). Este componente solo evita que un usuario normal vea
 * un formulario que de todas formas el backend le va a rechazar.
 */
export default function AdminRoute() {
  const { user } = useAuth();

  if (user?.role !== "ADMIN") {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
