import { ESTADO_CONFIG } from "../../utils/constants";

/**
 * Badge generico de texto/color (uso libre).
 * @param {{ children: React.ReactNode, className?: string }} props
 */
export function Badge({ children, className = "" }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${className}`}
    >
      {children}
    </span>
  );
}

/**
 * Badge especializado para el estado de un ProcesoLicitacion
 * (CREADO | ANALIZANDO | COMPLETADO | ERROR), con color semantico.
 * @param {{ estado: string }} props
 */
export function EstadoBadge({ estado }) {
  const config = ESTADO_CONFIG[estado] ?? {
    label: estado ?? "Desconocido",
    textClass: "text-slate-600",
    bgClass: "bg-slate-100",
    dotClass: "bg-slate-400",
  };

  return (
    <Badge className={`${config.bgClass} ${config.textClass}`}>
      <span className={`mr-1.5 h-1.5 w-1.5 rounded-full ${config.dotClass}`} />
      {config.label}
    </Badge>
  );
}

export default Badge;
