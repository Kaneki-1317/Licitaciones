import { useState } from "react";
import { useNavigate, useParams, Link } from "react-router-dom";
import { useProceso } from "../hooks/useProcesos";
import { useAnalisis } from "../hooks/useAnalisis";
import FileDropzone from "../components/FileDropzone/FileDropzone";
import Button from "../components/Button/Button";
import Modal from "../components/Modal/Modal";
import ErrorAlert from "../components/ErrorAlert/ErrorAlert";
import ProgressBar from "../components/ProgressBar/ProgressBar";
import Spinner from "../components/Spinner/Spinner";
import { EstadoBadge } from "../components/Badge/Badge";

/**
 * Pantalla de carga de documentos + analisis de un proceso:
 *   - fase "idle": seleccion de documentos (carpeta/drag&drop) y confirmacion
 *   - fase "analizando": espera bloqueante de POST /{id}/analizar
 *   - fase "completado": redirige a /procesos/{id}/resultado
 *   - fase "error": permite reintentar con los mismos archivos
 */
export default function AnalizarDocumentos() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { proceso, loading: cargandoProceso } = useProceso(id);
  const { fase, error, iniciarAnalisis, reset } = useAnalisis(id);

  const [archivos, setArchivos] = useState([]);
  const [mostrarConfirmacion, setMostrarConfirmacion] = useState(false);

  const analizando = fase === "analizando";

  const ejecutarAnalisis = async () => {
    setMostrarConfirmacion(false);
    try {
      await iniciarAnalisis(archivos);
      navigate(`/procesos/${id}/resultado`);
    } catch {
      // El estado de error ya queda expuesto por el hook (fase === "error").
    }
  };

  if (cargandoProceso) {
    return (
      <div className="flex justify-center py-16">
        <Spinner tamano="lg" />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-semibold text-slate-900">
            {proceso?.nombreProceso ?? `Proceso #${id}`}
          </h1>
          {proceso && <EstadoBadge estado={proceso.estado} />}
        </div>
        <p className="mt-1 text-sm text-slate-500">
          Selecciona los documentos del proceso para iniciar el análisis automático.
        </p>
      </div>

      {fase === "analizando" && (
        <div className="space-y-4 rounded-xl border border-slate-200 bg-white p-8 text-center">
          <Spinner tamano="lg" className="mx-auto" />
          <div>
            <p className="text-base font-medium text-slate-800">Analizando documentos…</p>
            <p className="mt-1 text-sm text-slate-500">
              El análisis puede tardar varios minutos mientras se procesan los documentos y se genera
              el Excel final. No cierres ni recargues esta página.
            </p>
          </div>
          <ProgressBar />
        </div>
      )}

      {fase === "error" && (
        <ErrorAlert error={error} onRetry={() => reset()} />
      )}

      {(fase === "idle" || fase === "error") && (
        <>
          <FileDropzone archivos={archivos} onChange={setArchivos} disabled={analizando} />

          <div className="flex justify-end gap-3">
            <Link to={`/procesos/${id}`}>
              <Button variant="secondary">Cancelar</Button>
            </Link>
            <Button
              variant="primary"
              disabled={analizando}
              onClick={() => setMostrarConfirmacion(true)}
            >
              Iniciar análisis
            </Button>
          </div>
        </>
      )}

      <Modal
        open={mostrarConfirmacion}
        title="Confirmar inicio de análisis"
        onClose={() => setMostrarConfirmacion(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setMostrarConfirmacion(false)}>
              Cancelar
            </Button>
            <Button variant="primary" onClick={ejecutarAnalisis}>
              Sí, iniciar análisis
            </Button>
          </>
        }
      >
        {archivos.length === 0 ? (
          <p>
            No seleccionaste ningún documento. ¿Deseas iniciar el análisis de todas formas?
          </p>
        ) : (
          <p>
            Se enviarán <strong>{archivos.length}</strong> documento{archivos.length === 1 ? "" : "s"} para
            análisis. Esta operación puede tardar varios minutos y no podrás modificar la selección
            mientras se procesa. ¿Deseas continuar?
          </p>
        )}
      </Modal>
    </div>
  );
}
