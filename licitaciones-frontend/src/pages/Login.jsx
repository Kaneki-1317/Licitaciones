import { useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import Button from "../components/Button/Button";

/**
 * Unica pantalla de autenticacion de la aplicacion (ver seccion 8 del
 * pedido): solo usuario + contraseña + "Iniciar sesión". NO hay
 * "Registrarse"/"Crear cuenta"/"¿No tienes cuenta?" ni login con
 * proveedores externos: no existe registro publico, el unico usuario es
 * el administrador inicial que crea el backend.
 */
export default function Login() {
  const { isAuthenticated, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const sesionExpirada = new URLSearchParams(location.search).get("sessionExpired") === "1";
  const rutaDestino = location.state?.from?.pathname ?? "/";

  // Si ya hay sesion (ej. el usuario entra manualmente a /login), no tiene
  // sentido mostrar el formulario: lo mandamos directo a donde iba.
  if (isAuthenticated) {
    return <Navigate to={rutaDestino} replace />;
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!username.trim() || !password) {
      setError("Usuario y contraseña son obligatorios");
      return;
    }
    setError(null);
    setLoading(true);
    try {
      await login(username.trim(), password);
      navigate(rutaDestino, { replace: true });
    } catch (err) {
      setError(err?.message ?? "Ocurrió un error inesperado. Intenta nuevamente.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
      <div className="w-full max-w-sm rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
        <h1 className="text-center text-2xl font-semibold text-slate-900">Inicio de sesión</h1>

        {sesionExpirada && (
          <p className="mt-4 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800" role="alert">
            Tu sesión ha expirado. Inicia sesión nuevamente.
          </p>
        )}

        <form onSubmit={handleSubmit} className="mt-6 space-y-4">
          <div>
            <label htmlFor="username" className="block text-sm font-medium text-slate-700">
              Usuario
            </label>
            <input
              id="username"
              type="text"
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoFocus
              className="mt-1.5 block w-full rounded-lg border border-slate-300 px-3 py-2 text-sm
                focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
            />
          </div>

          <div>
            <label htmlFor="password" className="block text-sm font-medium text-slate-700">
              Contraseña
            </label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="mt-1.5 block w-full rounded-lg border border-slate-300 px-3 py-2 text-sm
                focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
            />
          </div>

          {error && (
            <p className="text-sm text-red-600" role="alert">
              {error}
            </p>
          )}

          <Button type="submit" variant="primary" loading={loading} className="w-full">
            Iniciar sesión
          </Button>
        </form>
      </div>
    </div>
  );
}
