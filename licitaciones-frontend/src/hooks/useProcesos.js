import { useCallback, useEffect, useState } from "react";
import * as licitacionService from "../services/licitacionService";

/**
 * Lista el historial completo de procesos (usado por el Dashboard).
 * GET /api/licitaciones
 */
export function useProcesos() {
  const [procesos, setProcesos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    let cancelado = false;

    licitacionService
      .listarProcesos()
      .then((data) => {
        if (!cancelado) setProcesos(data);
      })
      .catch((err) => {
        if (!cancelado) setError(err);
      })
      .finally(() => {
        if (!cancelado) setLoading(false);
      });

    return () => {
      cancelado = true;
    };
  }, [version]);

  const refetch = useCallback(() => {
    setLoading(true);
    setError(null);
    setVersion((v) => v + 1);
  }, []);

  return { procesos, loading, error, refetch };
}

/**
 * Consulta el detalle de un proceso puntual (usado por DetalleProceso).
 * GET /api/licitaciones/{id}
 * @param {number|string} id
 */
export function useProceso(id) {
  const [proceso, setProceso] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    if (!id) return undefined;
    let cancelado = false;

    licitacionService
      .obtenerProceso(id)
      .then((data) => {
        if (!cancelado) setProceso(data);
      })
      .catch((err) => {
        if (!cancelado) setError(err);
      })
      .finally(() => {
        if (!cancelado) setLoading(false);
      });

    return () => {
      cancelado = true;
    };
  }, [id, version]);

  const refetch = useCallback(() => {
    setLoading(true);
    setError(null);
    setVersion((v) => v + 1);
  }, []);

  return { proceso, loading, error, refetch };
}

/**
 * Mutacion para crear un proceso nuevo (usado por CrearProceso).
 * POST /api/licitaciones
 */
export function useCrearProceso() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const crear = useCallback(async (nombreProceso) => {
    setLoading(true);
    setError(null);
    try {
      const proceso = await licitacionService.crearProceso(nombreProceso);
      return proceso;
    } catch (err) {
      setError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  return { crear, loading, error };
}
