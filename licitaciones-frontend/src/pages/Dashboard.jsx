import { useState } from "react";
import { Link } from "react-router-dom";
import { useProcesos } from "../hooks/useProcesos";
import * as licitacionService from "../services/licitacionService";
import { ESTADOS_PROCESO } from "../utils/constants";
import { formatDateTime } from "../utils/formatters";
import Table from "../components/Table/Table";
import { EstadoBadge } from "../components/Badge/Badge";
import Button from "../components/Button/Button";
import ErrorAlert from "../components/ErrorAlert/ErrorAlert";
import Spinner from "../components/Spinner/Spinner";

/**
 * Historial de procesos de licitacion. GET /api/licitaciones
 */
export default function Dashboard() {
  const { procesos, loading, error, refetch } = useProcesos();
  const [descargandoId, setDescargandoId] = useState(null);
  const [errorDescarga, setErrorDescarga] = useState(null);

  const handleDescargar = async (id) => {
    setErrorDescarga(null);
    setDescargandoId(id);
    try {
      await licitacionService.descargarExcel(id);
    } catch (err) {
      setErrorDescarga(err);
    } finally {
      setDescargandoId(null);
    }
  };

  const columnas = [
    { key: "nombreProceso", header: "Nombre" },
    {
      key: "estado",
      header: "Estado",
      render: (row) => <EstadoBadge estado={row.estado} />,
    },
    {
      key: "fechaCreacion",
      header: "Fecha creación",
      render: (row) => formatDateTime(row.fechaCreacion),
    },
    {
      key: "fechaFinalizacion",
      header: "Fecha finalización",
      render: (row) => formatDateTime(row.fechaFinalizacion),
    },
    {
      key: "acciones",
      header: "Acciones",
      render: (row) => (
        <div className="flex items-center gap-2">
          <Link to={`/procesos/${row.id}`}>
            <Button variant="secondary" size="sm">
              Ver detalle
            </Button>
          </Link>
          <Button
            variant="ghost"
            size="sm"
            disabled={row.estado !== ESTADOS_PROCESO.COMPLETADO}
            loading={descargandoId === row.id}
            onClick={() => handleDescargar(row.id)}
          >
            Descargar Excel
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Procesos de licitación</h1>
          <p className="mt-1 text-sm text-slate-500">
            Historial de procesos creados y su estado de análisis.
          </p>
        </div>
        <Link to="/procesos/nuevo">
          <Button variant="primary">+ Nuevo proceso</Button>
        </Link>
      </div>

      {errorDescarga && <ErrorAlert error={errorDescarga} />}
      {error && <ErrorAlert error={error} onRetry={refetch} />}

      {loading ? (
        <div className="flex justify-center py-16">
          <Spinner tamano="lg" />
        </div>
      ) : (
        <Table
          columns={columnas}
          data={procesos}
          getRowKey={(row) => row.id}
          emptyMessage="Todavía no hay procesos creados. Crea el primero para comenzar."
        />
      )}
    </div>
  );
}
