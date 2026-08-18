import { useCallback, useState } from "react";
import * as usuarioService from "../services/usuarioService";

/**
 * Mutacion para crear un usuario nuevo (solo ADMIN, ver pages/Usuarios.jsx).
 * POST /api/usuarios
 */
export function useCrearUsuario() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const crear = useCallback(async (username, password, rol) => {
    setLoading(true);
    setError(null);
    try {
      const usuario = await usuarioService.crearUsuario(username, password, rol);
      return usuario;
    } catch (err) {
      setError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  return { crear, loading, error };
}
