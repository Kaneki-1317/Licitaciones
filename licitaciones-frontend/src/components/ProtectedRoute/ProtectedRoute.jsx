import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";

/**
 * Protege todas las rutas hijas (ver seccion 10 del pedido): si no hay
 * sesion, redirige a /login guardando la ruta original en location.state
 * para volver ahi despues de iniciar sesion (ver Login.jsx). Si ya hay
 * sesion, renderiza normalmente las rutas hijas via <Outlet />.
 */
export default function ProtectedRoute() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <Outlet />;
}
