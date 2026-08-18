import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useCrearProceso } from "../hooks/useProcesos";
import Button from "../components/Button/Button";
import ErrorAlert from "../components/ErrorAlert/ErrorAlert";

const MAX_LENGTH = 255;

/**
 * Formulario de creacion de un proceso de licitacion.
 * POST /api/licitaciones -> redirige al flujo de carga de documentos.
 */
export default function CrearProceso() {
  const navigate = useNavigate();
  const { crear, loading, error } = useCrearProceso();
  const [nombreProceso, setNombreProceso] = useState("");
  const [errorValidacion, setErrorValidacion] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const valor = nombreProceso.trim();

    if (!valor) {
      setErrorValidacion("El nombre del proceso es obligatorio");
      return;
    }
    if (valor.length > MAX_LENGTH) {
      setErrorValidacion(`El nombre del proceso no puede superar ${MAX_LENGTH} caracteres`);
      return;
    }
    setErrorValidacion(null);

    try {
      const proceso = await crear(valor);
      navigate(`/procesos/${proceso.id}/analizar`);
    } catch {
      // el error ya queda expuesto por el hook (ErrorAlert lo muestra)
    }
  };

  return (
    <div className="mx-auto max-w-xl">
      <h1 className="text-2xl font-semibold text-slate-900">Nuevo proceso de licitación</h1>
      <p className="mt-1 text-sm text-slate-500">
        Crea el proceso para luego cargar sus documentos y ejecutar el análisis.
      </p>

      <form onSubmit={handleSubmit} className="mt-6 space-y-4 rounded-xl border border-slate-200 bg-white p-6">
        <div>
          <label htmlFor="nombreProceso" className="block text-sm font-medium text-slate-700">
            Nombre del proceso
          </label>
          <input
            id="nombreProceso"
            type="text"
            value={nombreProceso}
            onChange={(e) => setNombreProceso(e.target.value)}
            maxLength={MAX_LENGTH}
            placeholder="Ej. Licitación pública LP-001-2026"
            className="mt-1.5 block w-full rounded-lg border border-slate-300 px-3 py-2 text-sm
              focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          />
          <p className="mt-1 text-right text-xs text-slate-400">
            {nombreProceso.length}/{MAX_LENGTH}
          </p>
        </div>

        {errorValidacion && (
          <p className="text-sm text-red-600" role="alert">
            {errorValidacion}
          </p>
        )}
        <ErrorAlert error={error} />

        <div className="flex justify-end gap-3 pt-2">
          <Button type="submit" variant="primary" loading={loading}>
            Crear y continuar
          </Button>
        </div>
      </form>
    </div>
  );
}
