const ALINEACION = {
  left: "text-left",
  center: "text-center",
  right: "text-right",
};

/**
 * Tabla generica y reutilizable.
 * @param {{
 *   columns: Array<{ key: string, header: string, render?: (row: object) => React.ReactNode, className?: string, align?: "left"|"center"|"right" }>,
 *   data: object[],
 *   getRowKey?: (row: object, index: number) => string|number,
 *   emptyMessage?: string,
 * }} props
 */
export default function Table({ columns, data, getRowKey, emptyMessage = "No hay datos para mostrar." }) {
  if (!data || data.length === 0) {
    return (
      <div className="rounded-lg border border-dashed border-slate-300 bg-white py-12 text-center text-sm text-slate-500">
        {emptyMessage}
      </div>
    );
  }

  return (
    <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
      <table className="min-w-full divide-y divide-slate-200 text-sm">
        <thead className="bg-slate-50">
          <tr className="divide-x divide-slate-200">
            {columns.map((col) => (
              <th
                key={col.key}
                scope="col"
                className={`px-4 py-3 text-xs font-semibold uppercase tracking-wide text-slate-500 ${
                  ALINEACION[col.align] ?? "text-left"
                } ${col.className ?? ""}`}
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {data.map((row, index) => (
            <tr key={getRowKey ? getRowKey(row, index) : index} className="divide-x divide-slate-200 hover:bg-slate-50">
              {columns.map((col) => (
                <td
                  key={col.key}
                  className={`px-4 py-3 align-top text-slate-700 ${ALINEACION[col.align] ?? "text-left"} ${col.className ?? ""}`}
                >
                  {col.render ? col.render(row) : (row[col.key] ?? "—")}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
