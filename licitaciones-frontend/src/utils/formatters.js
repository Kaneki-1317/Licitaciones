/**
 * Formatea una fecha/hora ISO (LocalDateTime del backend, sin zona
 * horaria) para mostrarla en pantalla. Devuelve "—" si no hay valor
 * (por ejemplo, fechaFinalizacion en un proceso que aun no termino).
 * @param {string|null|undefined} isoString
 */
export function formatDateTime(isoString) {
  if (!isoString) return "—";
  const fecha = new Date(isoString);
  if (Number.isNaN(fecha.getTime())) return "—";
  return new Intl.DateTimeFormat("es-CO", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(fecha);
}

/**
 * Formatea un valor numerico como moneda (COP), tal como llega el campo
 * "presupuesto" de FichaTecnicaDTO. Devuelve "—" si es nulo/indefinido.
 * @param {number|string|null|undefined} valor
 */
export function formatCurrency(valor) {
  if (valor === null || valor === undefined || valor === "") return "—";
  const numero = typeof valor === "string" ? Number(valor) : valor;
  if (Number.isNaN(numero)) return String(valor);
  return new Intl.NumberFormat("es-CO", {
    style: "currency",
    currency: "COP",
    maximumFractionDigits: 0,
  }).format(numero);
}

/**
 * Formatea un valor decimal (0-1) como porcentaje, tal como llegan "roe" y
 * "roa" dentro de FichaTecnicaDTO.requisitosFinancieros. Devuelve "—" si es
 * nulo/indefinido.
 * @param {number|string|null|undefined} valor
 */
export function formatPercent(valor) {
  if (valor === null || valor === undefined || valor === "") return "—";
  const numero = typeof valor === "string" ? Number(valor) : valor;
  if (Number.isNaN(numero)) return String(valor);
  return new Intl.NumberFormat("es-CO", {
    style: "percent",
    maximumFractionDigits: 1,
  }).format(numero);
}

/**
 * Formatea un indicador financiero tipo razon (indiceLiquidez,
 * nivelEndeudamiento, coberturaIntereses) con 2 decimales fijos. Devuelve
 * "—" si es nulo/indefinido.
 * @param {number|string|null|undefined} valor
 */
export function formatRatio(valor) {
  if (valor === null || valor === undefined || valor === "") return "—";
  const numero = typeof valor === "string" ? Number(valor) : valor;
  if (Number.isNaN(numero)) return String(valor);
  return numero.toLocaleString("es-CO", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

/**
 * Formatea el porcentaje requerido de una poliza/garantia (PolizaDTO.valor
 * del backend): un entero "pelado" que ya representa el porcentaje
 * directamente (10 significa 10%, a diferencia de formatPercent que espera
 * una fraccion 0-1 como roe/roa). 0 es un valor valido (poliza no exigida
 * para ese proceso) y debe mostrarse como "0%", no tratarse como dato
 * faltante; por eso se valida con typeof en vez de un chequeo "truthy".
 * @param {number|null|undefined} valor
 */
export function formatPolizaPorcentaje(valor) {
  if (typeof valor !== "number" || Number.isNaN(valor)) return "No disponible";
  return `${valor}%`;
}

/**
 * Formatea un tamano en bytes a una unidad legible (KB, MB, GB).
 * @param {number} bytes
 */
export function formatBytes(bytes) {
  if (!Number.isFinite(bytes) || bytes < 0) return "—";
  if (bytes === 0) return "0 B";
  const unidades = ["B", "KB", "MB", "GB"];
  const exponente = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), unidades.length - 1);
  const valor = bytes / 1024 ** exponente;
  return `${valor.toFixed(exponente === 0 ? 0 : 1)} ${unidades[exponente]}`;
}

/**
 * Extrae la extension de un nombre de archivo en mayusculas (ej. "PDF").
 * Devuelve "ARCHIVO" si no tiene extension detectable.
 * @param {string} nombreArchivo
 */
export function getFileExtension(nombreArchivo) {
  if (!nombreArchivo) return "ARCHIVO";
  const partes = nombreArchivo.split(".");
  if (partes.length < 2) return "ARCHIVO";
  return partes[partes.length - 1].toUpperCase();
}
