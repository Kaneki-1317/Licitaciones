const TAMANOS = {
  sm: "h-4 w-4 border-2",
  md: "h-8 w-8 border-2",
  lg: "h-12 w-12 border-[3px]",
};

/**
 * Spinner circular generico.
 * @param {{ tamano?: "sm"|"md"|"lg", className?: string }} props
 */
export default function Spinner({ tamano = "md", className = "" }) {
  return (
    <span
      role="status"
      aria-label="Cargando"
      className={`inline-block animate-spin rounded-full border-slate-200 border-t-brand-600 ${TAMANOS[tamano]} ${className}`}
    />
  );
}
