import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";

const enlaceClase = ({ isActive }) =>
  `rounded-md px-3 py-2 text-sm font-medium transition-colors ${
    isActive ? "bg-brand-50 text-brand-700" : "text-slate-600 hover:bg-slate-100"
  }`;

const enlacePrimarioClase = ({ isActive }) =>
  `rounded-lg px-3.5 py-2 text-sm font-medium text-white transition-colors ${
    isActive ? "bg-brand-700" : "bg-brand-600 hover:bg-brand-700"
  }`;

/**
 * Layout general de la aplicacion: cabecera con navegacion + contenedor
 * de contenido. Las rutas hijas se renderizan en <Outlet />. Solo se
 * renderiza para rutas ya protegidas por <ProtectedRoute /> (ver App.jsx),
 * asi que siempre hay sesion aqui.
 */
export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    // Cierra sesion unicamente: no borra procesos ni ningun otro dato,
    // solo el token/usuario guardados en el navegador (ver seccion 13).
    logout();
    navigate("/login", { replace: true });
  };

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3 sm:px-6">
          <NavLink to="/" className="flex items-center gap-2 text-slate-900">
            <span className="text-lg font-semibold">Automatizacion de Licitaciones</span>
          </NavLink>

          <nav className="flex items-center gap-1">
            <NavLink to="/" end className={enlaceClase}>
              Procesos
            </NavLink>
            <NavLink to="/procesos/nuevo" className={enlacePrimarioClase}>
              + Nuevo proceso
            </NavLink>
            {user?.role === "ADMIN" && (
              <NavLink to="/usuarios" className={enlaceClase}>
                Usuarios
              </NavLink>
            )}

            <span className="ml-2 hidden text-sm text-slate-500 sm:inline">{user?.username}</span>
            <button
              type="button"
              onClick={handleLogout}
              className="rounded-md px-3 py-2 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100"
            >
              Cerrar sesión
            </button>
          </nav>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <Outlet />
      </main>
    </div>
  );
}
