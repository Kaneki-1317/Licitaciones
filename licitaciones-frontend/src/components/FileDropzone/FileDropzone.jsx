import { useRef, useState } from "react";
import { formatBytes, getFileExtension } from "../../utils/formatters";
import Button from "../Button/Button";

/**
 * Selector de documentos para el analisis: permite elegir una carpeta
 * completa (input con atributo webkitdirectory), archivos sueltos, o
 * arrastrarlos (drag & drop). Lista lo seleccionado con tamano/extension
 * y permite quitar archivos antes de enviar.
 *
 * @param {{
 *   archivos: File[],
 *   onChange: (archivos: File[]) => void,
 *   disabled?: boolean,
 * }} props
 */
export default function FileDropzone({ archivos, onChange, disabled = false }) {
  const [arrastrando, setArrastrando] = useState(false);
  const inputCarpetaRef = useRef(null);
  const inputArchivosRef = useRef(null);

  const agregarArchivos = (nuevos) => {
    const lista = Array.from(nuevos);
    if (lista.length === 0) return;
    onChange([...archivos, ...lista]);
  };

  const quitarArchivo = (index) => {
    onChange(archivos.filter((_, i) => i !== index));
  };

  const limpiarTodo = () => onChange([]);

  const handleDrop = (e) => {
    e.preventDefault();
    setArrastrando(false);
    if (disabled) return;
    agregarArchivos(e.dataTransfer.files);
  };

  const tamanoTotal = archivos.reduce((total, archivo) => total + archivo.size, 0);

  return (
    <div className="space-y-4">
      <div
        onDragOver={(e) => {
          e.preventDefault();
          if (!disabled) setArrastrando(true);
        }}
        onDragLeave={() => setArrastrando(false)}
        onDrop={handleDrop}
        className={`flex flex-col items-center justify-center gap-3 rounded-xl border-2 border-dashed p-10 text-center transition-colors
          ${arrastrando ? "border-brand-500 bg-brand-50" : "border-slate-300 bg-white"}
          ${disabled ? "opacity-60" : ""}`}
      >
        <p className="text-sm text-slate-600">
          Arrastra aqui los documentos del proceso, o selecciona una carpeta completa
        </p>

        <div className="flex flex-wrap justify-center gap-3">
          <Button
            variant="primary"
            size="sm"
            disabled={disabled}
            onClick={() => inputCarpetaRef.current?.click()}
          >
            Seleccionar carpeta
          </Button>
          <Button
            variant="secondary"
            size="sm"
            disabled={disabled}
            onClick={() => inputArchivosRef.current?.click()}
          >
            Seleccionar archivos
          </Button>
        </div>

        {/* Selector de carpeta completa (Chrome/Edge/Firefox soportan webkitdirectory) */}
        <input
          ref={inputCarpetaRef}
          type="file"
          webkitdirectory=""
          directory=""
          multiple
          className="hidden"
          onChange={(e) => {
            agregarArchivos(e.target.files);
            e.target.value = "";
          }}
        />
        {/* Selector de archivos individuales */}
        <input
          ref={inputArchivosRef}
          type="file"
          multiple
          className="hidden"
          onChange={(e) => {
            agregarArchivos(e.target.files);
            e.target.value = "";
          }}
        />
      </div>

      {archivos.length > 0 && (
        <div className="rounded-lg border border-slate-200 bg-white">
          <div className="flex items-center justify-between border-b border-slate-200 px-4 py-2.5">
            <p className="text-sm font-medium text-slate-700">
              {archivos.length} archivo{archivos.length === 1 ? "" : "s"} seleccionado{archivos.length === 1 ? "" : "s"}{" "}
              <span className="text-slate-400">· {formatBytes(tamanoTotal)}</span>
            </p>
            <button
              type="button"
              onClick={limpiarTodo}
              disabled={disabled}
              className="text-xs font-medium text-red-600 hover:underline disabled:opacity-50"
            >
              Quitar todos
            </button>
          </div>

          <ul className="max-h-72 divide-y divide-slate-100 overflow-y-auto">
            {archivos.map((archivo, index) => (
              <li
                key={`${archivo.name}-${archivo.size}-${archivo.lastModified}-${index}`}
                className="flex items-center justify-between gap-3 px-4 py-2.5 text-sm"
              >
                <div className="flex min-w-0 items-center gap-3">
                  <span className="shrink-0 rounded bg-slate-100 px-2 py-1 text-[11px] font-semibold text-slate-500">
                    {getFileExtension(archivo.name)}
                  </span>
                  <span className="truncate text-slate-700" title={archivo.webkitRelativePath || archivo.name}>
                    {archivo.webkitRelativePath || archivo.name}
                  </span>
                </div>
                <div className="flex shrink-0 items-center gap-3">
                  <span className="text-slate-400">{formatBytes(archivo.size)}</span>
                  <button
                    type="button"
                    onClick={() => quitarArchivo(index)}
                    disabled={disabled}
                    aria-label={`Quitar ${archivo.name}`}
                    className="text-slate-400 hover:text-red-600 disabled:opacity-50"
                  >
                    ✕
                  </button>
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
