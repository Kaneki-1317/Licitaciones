/**
 * Muestra un error normalizado por utils/errorHandler.js
 * ({ status, error, mensaje, detalles }), replicando el contrato
 * ErrorResponseDTO del backend.
 * @param {{ error: { status?: number, error?: string, mensaje: string, detalles?: string[] } | null, onRetry?: () => void }} props
 */
export default function ErrorAlert({ error, onRetry }) {
  if (!error) return null;

  return (
    <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800">
      <p className="font-semibold">
        {error.error ?? "Error"}
        {error.status ? ` (${error.status})` : ""}
      </p>
      <p className="mt-1">{error.mensaje}</p>

      {Array.isArray(error.detalles) && error.detalles.length > 0 && (
        <ul className="mt-2 list-inside list-disc space-y-0.5">
          {error.detalles.map((detalle) => (
            <li key={detalle}>{detalle}</li>
          ))}
        </ul>
      )}

      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="mt-3 rounded-md bg-red-100 px-3 py-1.5 text-sm font-medium text-red-800 hover:bg-red-200"
        >
          Reintentar
        </button>
      )}
    </div>
  );
}
