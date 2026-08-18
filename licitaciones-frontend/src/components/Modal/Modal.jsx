import { useEffect } from "react";

/**
 * Dialogo modal generico centrado, con overlay. Se cierra con la tecla
 * Escape, con click en el overlay o con el boton de cierre.
 * @param {{
 *   open: boolean,
 *   title?: string,
 *   onClose: () => void,
 *   children: React.ReactNode,
 *   footer?: React.ReactNode,
 * }} props
 */
export default function Modal({ open, title, onClose, children, footer }) {
  useEffect(() => {
    if (!open) return;
    const handleKeyDown = (e) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-slate-900/50" onClick={onClose} aria-hidden="true" />
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className="relative w-full max-w-md rounded-xl bg-white p-6 shadow-xl"
      >
        <div className="mb-4 flex items-start justify-between gap-4">
          {title && <h2 className="text-lg font-semibold text-slate-900">{title}</h2>}
          <button
            type="button"
            onClick={onClose}
            aria-label="Cerrar"
            className="ml-auto rounded-md p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
          >
            ✕
          </button>
        </div>

        <div className="text-sm text-slate-600">{children}</div>

        {footer && <div className="mt-6 flex justify-end gap-3">{footer}</div>}
      </div>
    </div>
  );
}
