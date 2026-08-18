/**
 * Barra de progreso indeterminada: el backend no reporta un porcentaje de
 * avance real durante el analisis (POST /analizar es una llamada
 * sincrona y bloqueante), por lo que solo se comunica "hay actividad".
 */
export default function ProgressBar() {
  return (
    <div
      role="progressbar"
      aria-label="Analizando documentos"
      className="h-2 w-full overflow-hidden rounded-full bg-slate-200"
    >
      <div className="h-full w-1/3 rounded-full bg-brand-600 animate-progress-indeterminate" />
    </div>
  );
}
