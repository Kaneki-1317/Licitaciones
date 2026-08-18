import { formatCurrency } from "./formatters";

/**
 * FuenteDTO (campo/archivo/pagina) no incluye el valor extraido, solo su
 * ubicacion. Para los campos que SI existen en FichaTecnicaDTO podemos
 * mostrar el valor real haciendo un cruce por nombre de campo; para el
 * resto, se indica explicitamente que el backend no expone ese dato (no
 * se inventa informacion que la API no entrega).
 */
const CAMPOS_FICHA_TECNICA = {
  entidad: "entidad",
  "numero de proceso": "numeroProceso",
  objeto: "objeto",
  presupuesto: "presupuesto",
  plazo: "plazo",
};

// Reemplazo directo de vocales/letras acentuadas, sin depender de
// normalize() ni de rangos Unicode en el codigo fuente.
const VOCALES_ACENTUADAS = { "á": "a", "é": "e", "í": "i", "ó": "o", "ú": "u", "ñ": "n" };

function normalizar(texto) {
  return texto
    .trim()
    .toLowerCase()
    .split("")
    .map((c) => VOCALES_ACENTUADAS[c] ?? c)
    .join("");
}

/**
 * Intenta resolver el valor encontrado para una fuente de trazabilidad,
 * cruzando fuente.campo contra los campos conocidos de FichaTecnicaDTO.
 * @param {{campo: string}} fuente
 * @param {object|null} fichaTecnica
 * @returns {string|null} el valor formateado, o null si el backend no lo expone
 */
export function resolverValorFuente(fuente, fichaTecnica) {
  if (!fichaTecnica || !fuente?.campo) return null;

  const key = CAMPOS_FICHA_TECNICA[normalizar(fuente.campo)];
  if (!key) return null;

  const valor = fichaTecnica[key];
  if (valor === null || valor === undefined || valor === "") return null;

  return key === "presupuesto" ? formatCurrency(valor) : String(valor);
}

/**
 * Formatea la trazabilidad de pagina de un campo/item, tal como la envia el
 * backend: un numero simple ({@code "pagina": 3}) o un arreglo de numeros
 * ({@code "pagina": [6, 25]}) cuando el dato aparece en mas de una pagina.
 * Nunca inventa un numero: si no hay pagina disponible (null/undefined/vacio/
 * tipo inesperado) cae a "Página no disponible" en vez de omitir la linea o
 * mostrar un valor arbitrario.
 *
 * @param {number|number[]|null|undefined} pagina
 * @returns {string} "Página N", "Páginas N, M, ..." o "Página no disponible"
 */
export function formatPages(pagina) {
  if (Array.isArray(pagina)) {
    const paginasValidas = pagina.filter((p) => typeof p === "number" && Number.isFinite(p));
    if (paginasValidas.length === 0) return "Página no disponible";
    return paginasValidas.length === 1
      ? `Página ${paginasValidas[0]}`
      : `Páginas ${paginasValidas.join(", ")}`;
  }
  if (typeof pagina === "number" && Number.isFinite(pagina)) return `Página ${pagina}`;
  return "Página no disponible";
}

/**
 * Normaliza un campo de fichaTecnica que llega envuelto con su pagina de
 * origen (CampoTrazableDTO del backend): {@code { valor, pagina } }, ej.
 * {@code { "valor": "Distrito Especial...", "pagina": 3 } }. Si el valor no
 * viene envuelto (string/numero/array "pelado", como nombreArchivo o
 * codigosRUP.valor) se devuelve tal cual, con pagina en null.
 *
 * Centraliza esta distincion en un solo lugar para que Resultado.jsx no
 * tenga que repetir el chequeo campo por campo. "pagina" puede ser un numero
 * o un arreglo de numeros (ver {@link formatPages}) segun el campo.
 * @param {*} valorCrudo
 * @returns {{ valor: *, pagina: number|number[]|null }}
 */
export function normalizarValorConFuente(valorCrudo) {
  const esCampoTrazable =
    valorCrudo !== null &&
    typeof valorCrudo === "object" &&
    !Array.isArray(valorCrudo) &&
    "valor" in valorCrudo;

  return esCampoTrazable
    ? { valor: valorCrudo.valor, pagina: valorCrudo.pagina ?? null }
    : { valor: valorCrudo, pagina: null };
}

// Los seis tipos de poliza/garantia dentro de data.polizas (PolizasDTO del
// backend). Nombres de campo identicos a los que envia el backend.
const CAMPOS_POLIZAS = [
  "seriedad",
  "cumplimiento",
  "buenManejoAnticipo",
  "pagoSalarios",
  "estabilidad",
  "calidad",
];

/**
 * Extrae el porcentaje numerico de una poliza individual
 * (data.polizas.seriedad, etc.), tal como lo envia HOY el backend: un
 * numero "pelado" ({@code "seriedad": 10}), SIN envoltorio
 * {valor, archivo, pagina} — la trazabilidad de polizas vive a nivel de TODO
 * el grupo (nombreArchivo/paginasConsultadas, ver normalizarPolizas), no por
 * poliza individual.
 *
 * 0 es un valor valido (poliza no exigida) y se preserva: se usa un chequeo
 * explicito de tipo (typeof === "number"), nunca "if (!valor)", para no
 * confundirlo con dato ausente.
 *
 * La rama de objeto ({@code valorCrudo.valor}) es solo compatibilidad
 * interna por si el backend llegara a envolver una poliza individual en el
 * futuro; hoy nunca ocurre, y de ocurrir tampoco se inventaria un archivo o
 * pagina individual a partir de ella — eso lo sigue resolviendo unicamente
 * normalizarPolizas() a nivel de grupo.
 * @param {*} valorCrudo
 * @returns {number|null}
 */
function extraerPorcentajePoliza(valorCrudo) {
  if (typeof valorCrudo === "number" && Number.isFinite(valorCrudo)) {
    return valorCrudo;
  }
  if (
    valorCrudo !== null &&
    typeof valorCrudo === "object" &&
    typeof valorCrudo.valor === "number" &&
    Number.isFinite(valorCrudo.valor)
  ) {
    return valorCrudo.valor;
  }
  return null;
}

/**
 * Normaliza data.polizas tal como lo envia el backend hoy:
 * {@code { "nombreArchivo": "...", "paginasConsultadas": [29, 37],
 *          "seriedad": 10, "cumplimiento": 10, "buenManejoAnticipo": 0,
 *          "pagoSalarios": 5, "estabilidad": 0, "calidad": 10 } }.
 *
 * La trazabilidad (archivo + paginas) es UNA sola, a nivel de todo el grupo
 * de polizas — el contrato actual no trae una pagina individual por poliza,
 * asi que nunca se le asigna una pagina de "paginasConsultadas" a una poliza
 * en particular (ej. NO se asume que "seriedad" salio de la pagina 29 solo
 * porque es la primera del arreglo).
 * @param {*} polizasCrudas
 * @returns {{
 *   nombreArchivo: string|null,
 *   paginasConsultadas: number[]|null,
 *   valores: Record<string, number|null>,
 * }}
 */
export function normalizarPolizas(polizasCrudas) {
  if (polizasCrudas === null || polizasCrudas === undefined || typeof polizasCrudas !== "object") {
    return { nombreArchivo: null, paginasConsultadas: null, valores: {} };
  }

  const nombreArchivo =
    typeof polizasCrudas.nombreArchivo === "string" && polizasCrudas.nombreArchivo.trim() !== ""
      ? polizasCrudas.nombreArchivo
      : null;

  let paginasConsultadas = null;
  if (Array.isArray(polizasCrudas.paginasConsultadas)) {
    const paginas = polizasCrudas.paginasConsultadas.filter(
      (p) => typeof p === "number" && Number.isFinite(p)
    );
    paginasConsultadas = paginas.length > 0 ? paginas : null;
  } else if (typeof polizasCrudas.paginasConsultadas === "number") {
    paginasConsultadas = [polizasCrudas.paginasConsultadas];
  }

  const valores = {};
  for (const campo of CAMPOS_POLIZAS) {
    valores[campo] = extraerPorcentajePoliza(polizasCrudas[campo]);
  }

  return { nombreArchivo, paginasConsultadas, valores };
}
