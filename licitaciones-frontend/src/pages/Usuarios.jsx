import { useState } from "react";
import { useCrearUsuario } from "../hooks/useCrearUsuario";
import Button from "../components/Button/Button";
import ErrorAlert from "../components/ErrorAlert/ErrorAlert";

const ROLES = ["ADMIN", "USER"];

/**
 * Creacion de usuarios (ver seccion 6/9 del pedido): solo visible/accesible
 * para ADMIN (ver components/AdminRoute y el link "Usuarios" en Layout).
 * La seguridad real vive en el backend (hasRole("ADMIN") en
 * security.SecurityConfig): esta pagina y el link que lleva a ella son solo
 * conveniencia de UI, no el control de acceso.
 *
 * No hay listado/edicion/borrado de usuarios: solo se pidio poder crear
 * usuarios nuevos. "Activo" siempre queda en true (lo decide el backend,
 * ver UsuarioService#crearUsuario), no es un campo del formulario.
 */
export default function Usuarios() {
  const { crear, loading, error } = useCrearUsuario();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [rol, setRol] = useState("USER");
  const [creado, setCreado] = useState(null);
  const [errorValidacion, setErrorValidacion] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setCreado(null);

    if (!username.trim() || !password) {
      setErrorValidacion("Usuario y contraseña son obligatorios");
      return;
    }
    setErrorValidacion(null);

    try {
      const usuario = await crear(username.trim(), password, rol);
      setCreado(usuario);
      setUsername("");
      setPassword("");
      setRol("USER");
    } catch {
      // el error ya queda expuesto por el hook (ErrorAlert lo muestra)
    }
  };

  return (
    <div className="mx-auto max-w-xl">
      <h1 className="text-2xl font-semibold text-slate-900">Usuarios</h1>
      <p className="mt-1 text-sm text-slate-500">
        Crea usuarios nuevos con acceso a la aplicación. Solo un administrador puede hacerlo.
      </p>

      <form onSubmit={handleSubmit} className="mt-6 space-y-4 rounded-xl border border-slate-200 bg-white p-6">
        <div>
          <label htmlFor="username" className="block text-sm font-medium text-slate-700">
            Usuario
          </label>
          <input
            id="username"
            type="text"
            autoComplete="off"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
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
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="mt-1.5 block w-full rounded-lg border border-slate-300 px-3 py-2 text-sm
              focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          />
        </div>

        <div>
          <label htmlFor="rol" className="block text-sm font-medium text-slate-700">
            Rol
          </label>
          <select
            id="rol"
            value={rol}
            onChange={(e) => setRol(e.target.value)}
            className="mt-1.5 block w-full rounded-lg border border-slate-300 px-3 py-2 text-sm
              focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          >
            {ROLES.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>
        </div>

        {errorValidacion && (
          <p className="text-sm text-red-600" role="alert">
            {errorValidacion}
          </p>
        )}
        <ErrorAlert error={error} />

        {creado && (
          <p className="rounded-lg border border-green-200 bg-green-50 p-3 text-sm text-green-800" role="status">
            Usuario "{creado.username}" creado con rol {creado.rol}.
          </p>
        )}

        <div className="flex justify-end gap-3 pt-2">
          <Button type="submit" variant="primary" loading={loading}>
            Crear usuario
          </Button>
        </div>
      </form>
    </div>
  );
}
