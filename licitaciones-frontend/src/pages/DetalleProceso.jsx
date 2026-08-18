import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useProceso } from "../hooks/useProcesos";
import * as licitacionService from "../services/licitacionService";
import { ESTADOS_PROCESO } from "../utils/constants";
import { formatDateTime } from "../utils/formatters";
import { EstadoBadge } from "../components/Badge/Badge";
import Button from "../components/Button/Button";
import ErrorAlert from "../components/ErrorAlert/ErrorAlert";
import Spinner from "../components/Spinner/Spinner";

/**
 * Detalle de un proceso: informacion general, estado, descarga de Excel y
 * acceso al resultado del analisis. GET /api/licitaciones/{id}
 */
export default function DetalleProceso() {
  const { id } = useParams();
  const { proceso, loading, error, refetch } = useProceso(id);
  const [descargando, setDescargando] = useState(false);
  const [errorDescarga, setErrorDescarga] = useState(null);

  const handleDescargar = async () => {
    setErrorDescarga(null);
    setDescargando(true);
    try {
      await licitacionService.descargarExcel(id);
    } catch (err) {
      setErrorDescarga(err);
    } finally {
      setDescargando(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center py-16">
        <Spinner tamano="lg" />
      </div>
    );
  }

  if (error) {
    return <ErrorAlert error={error} onRetry={refetch} />;
  }

  const completado = proceso.estado === ESTADOS_PROCESO.COMPLETADO;
  const enError = proceso.estado === ESTADOS_PROCESO.ERROR;
  const creado = proceso.estado === ESTADOS_PROCESO.CREADO;

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">{proceso.nombreProceso}</h1>
          <p className="mt-1 text-sm text-slate-500">Proceso #{proceso.id}</p>
        </div>
        <EstadoBadge estado={proceso.estado} />
      </div>

      {errorDescarga && <ErrorAlert error={errorDescarga} />}

      <dl className="grid grid-cols-1 gap-4 rounded-xl border border-slate-200 bg-white p-6 sm:grid-cols-2">
        <div>
          <dt className="text-xs font-medium uppercase tracking-wide text-slate-400">Fecha de creación</dt>
          <dd className="mt-1 text-sm text-slate-800">{formatDateTime(proceso.fechaCreacion)}</dd>
        </div>
        <div>
          <dt className="text-xs font-medium uppercase tracking-wide text-slate-400">Fecha de finalización</dt>
          <dd className="mt-1 text-sm text-slate-800">{formatDateTime(proceso.fechaFinalizacion)}</dd>
        </div>
      </dl>

      {enError && (
        <p className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          El último análisis de este proceso terminó en error. Puedes intentar analizarlo de nuevo.
        </p>
      )}

      <div className="flex flex-wrap gap-3">
        {(creado || enError) && (
          <Link to={`/procesos/${id}/analizar`}>
            <Button variant="primary">{enError ? "Reintentar análisis" : "Cargar documentos y analizar"}</Button>
          </Link>
        )}

        {(completado || enError) && (
          <Link to={`/procesos/${id}/resultado`}>
            <Button variant="secondary">Consultar resultado</Button>
          </Link>
        )}

        <Button variant="secondary" disabled={!completado} loading={descargando} onClick={handleDescargar}>
          Descargar Excel
        </Button>
      </div>
    </div>
  );
}
