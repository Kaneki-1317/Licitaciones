import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import * as licitacionService from "../services/licitacionService";
import { formatCurrency, formatPercent, formatPolizaPorcentaje, formatRatio } from "../utils/formatters";
import { formatPages, normalizarPolizas, normalizarValorConFuente } from "../utils/trazabilidad";
import Badge from "../components/Badge/Badge";
import Button from "../components/Button/Button";
import ErrorAlert from "../components/ErrorAlert/ErrorAlert";
import Spinner from "../components/Spinner/Spinner";
import Table from "../components/Table/Table";

// Campos de "Informacion general". Los que tienen "key" mapeado a una
// propiedad real de data.fichaTecnica (ver contrato de GET /{id}/resultado)
// se muestran con su valor; los que el backend todavia no envia (nombreProyecto,
// tipoProceso, presupuestoSmmlv, modalidadEjecucion) quedan con la clave lista
// para cuando n8n los incluya, mostrando "No disponible" mientras tanto.
const CAMPOS_FICHA = [
  { key: "nombreProyecto", label: "Nombre Proyecto" },
  { key: "tipoProceso", label: "Tipo de Proceso" },
  { key: "entidad", label: "Entidad" },
  { key: "objetoContrato", label: "Objeto del Contrato / Alcance" },
  { key: "presupuesto", label: "Presupuesto", formatter: formatCurrency },
  { key: "presupuestoSmmlv", label: "Presupuesto en SMMLV" },
  { key: "plazoProyecto", label: "Tiempo de Ejecución" },
  { key: "modalidadEjecucion", label: "Modalidad de ejecución" },
  { key: "ciudad", label: "Ciudad del proyecto" },
  { key: "valoracion", label: "Valoración" },
];

// requisitosFinancieros del backend: roe/roa llegan como fraccion (0.04 =
// 4%), el resto son razones financieras simples.
const INDICADORES_FINANCIEROS = [
  { key: "indiceLiquidez", label: "Índice de Liquidez", formatter: formatRatio },
  { key: "nivelEndeudamiento", label: "Índice de Endeudamiento", formatter: formatRatio },
  { key: "coberturaIntereses", label: "Razón de Cobertura a Intereses", formatter: formatRatio },
  { key: "roe", label: "Rentabilidad del Patrimonio (ROE)", formatter: formatPercent },
  { key: "roa", label: "Rentabilidad del Activo (ROA)", formatter: formatPercent },
];

/**
 * Formatea el valor de un campo de la ficha tecnica y resuelve la pagina de
 * la que se extrajo (trazabilidad), via normalizarValorConFuente: el
 * backend envuelve estos campos como {valor, pagina}. Si el campo no vino en
 * la respuesta o quedo vacio, el texto cae a "No disponible".
 * @returns {{ texto: string, pagina: number|number[]|null }}
 */
function mostrarCampo(valorCrudo, formatter) {
  const { valor, pagina } = normalizarValorConFuente(valorCrudo);
  const formateado = formatter ? formatter(valor) : valor;
  const texto =
    formateado === null || formateado === undefined || formateado === "" || formateado === "—"
      ? "No disponible"
      : formateado;
  return { texto, pagina };
}

/** Celda "Fuente" reutilizada en las tablas de documentacion: la(s) pagina(s) del item, via formatPages. */
function celdaPagina(row) {
  return <span className="text-xs italic text-slate-500">{formatPages(row.pagina)}</span>;
}

// capacidadJuridica y capacidadTecnica comparten exactamente la misma forma
// de item ({id, documento, fuente}), asi que reutilizan la misma definicion
// de columnas. El ID lo genera y numera el backend (n8n); el frontend solo
// lo muestra tal cual llega, sin recalcularlo ni reiniciarlo entre tablas.
const COLUMNAS_ID_DOCUMENTO = [
  { key: "id", header: "ID", align: "center", className: "w-16 font-medium text-slate-600" },
  { key: "documento", header: "Documento" },
  { key: "fuente", header: "Fuente", render: celdaPagina },
];

// Mismo item que COLUMNAS_ID_DOCUMENTO (el campo JSON sigue llamandose
// "documento"), mostrado bajo el encabezado "Criterio", mas la columna
// "Puntos" propia de esta categoria.
const COLUMNAS_CRITERIOS_EVALUACION = [
  { key: "id", header: "ID", align: "center", className: "w-16 font-medium text-slate-600" },
  { key: "documento", header: "Criterio" },
  { key: "puntos", header: "Puntos", align: "right", render: (row) => mostrarCampo(row.puntos).texto },
  { key: "fuente", header: "Fuente", render: celdaPagina },
];

// recursoHumanoRequerido no trae ID. "descripcion" es el campo del nuevo
// contrato; mientras el backend siga enviando el campo viejo "perfil" (sin
// descripcion ni fuente), se usa como respaldo para no perder informacion
// ya visible hoy.
const COLUMNAS_RECURSO_HUMANO = [
  { key: "cargo", header: "Cargo" },
  { key: "descripcion", header: "Descripción", render: (row) => mostrarCampo(row.descripcion ?? row.perfil).texto },
  { key: "fuente", header: "Fuente", render: celdaPagina },
];

// Los seis tipos de poliza/garantia dentro de data.polizas (PolizasDTO del
// backend). Nombres de campo identicos a los que envia el backend: cada uno
// es un porcentaje "pelado" (numero), no un objeto {valor, archivo, pagina}
// — la trazabilidad de todo el grupo vive en nombreArchivo/paginasConsultadas,
// ver normalizarPolizas en utils/trazabilidad.
const CAMPOS_POLIZAS = [
  { key: "seriedad", label: "Seriedad de la oferta" },
  { key: "cumplimiento", label: "Cumplimiento" },
  { key: "buenManejoAnticipo", label: "Buen manejo del anticipo" },
  { key: "pagoSalarios", label: "Pago de salarios y prestaciones sociales" },
  { key: "estabilidad", label: "Estabilidad" },
  { key: "calidad", label: "Calidad" },
];

/**
 * Resultado del analisis de un proceso. GET /api/licitaciones/{id}/resultado
 * { success, data: { fichaTecnica, documentacion, perfiles } }. Cada campo de
 * fichaTecnica ({valor, pagina}) y cada item de documentacion ({id, documento,
 * pagina}) trae su propia pagina de origen; "pagina" puede llegar como numero
 * o como arreglo de numeros (ver formatPages en utils/trazabilidad). La
 * seccion de Trazabilidad global (fuentes[]) sigue como placeholder.
 */
export default function Resultado() {
  const { id } = useParams();
  const [resultado, setResultado] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [descargando, setDescargando] = useState(false);
  const [errorDescarga, setErrorDescarga] = useState(null);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    let cancelado = false;

    licitacionService
      .consultarResultado(id)
      .then((data) => {
        if (!cancelado) setResultado(data);
      })
      .catch((err) => {
        if (!cancelado) setError(err);
      })
      .finally(() => {
        if (!cancelado) setLoading(false);
      });

    return () => {
      cancelado = true;
    };
  }, [id, version]);

  const cargar = useCallback(() => {
    setLoading(true);
    setError(null);
    setVersion((v) => v + 1);
  }, []);

  const handleDescargar = async () => {
    setErrorDescarga(null);
    setDescargando(true);
    try {
      await licitacionService.descargarExcel(id);
    } catch (err) {
      setErrorDescarga(err);
    } finally {
      setDescargando(false);
    }
  };

  // Estado "Procesando análisis": la peticion GET /resultado esta en curso.
  if (loading) {
    return (
      <div className="flex flex-col items-center gap-3 py-16">
        <Spinner tamano="lg" />
        <p className="text-sm text-slate-500">Procesando análisis…</p>
      </div>
    );
  }

  // Estado "Error": fallo la peticion HTTP (red, 409, 500, etc.).
  if (error) {
    return (
      <div className="mx-auto max-w-2xl space-y-4">
        <ErrorAlert error={error} onRetry={cargar} />
        {error.status === 409 && (
          <p className="text-sm text-slate-500">
            Este proceso todavía no tiene un análisis completado.{" "}
            <Link to={`/procesos/${id}/analizar`} className="text-brand-600 hover:underline">
              Ir a cargar documentos
            </Link>
          </p>
        )}
      </div>
    );
  }

  // Estado "Error": la peticion respondio 200 pero el backend reporta
  // success=false (ej. n8n termino con error de extraccion).
  if (resultado && resultado.success === false) {
    return (
      <div className="mx-auto max-w-2xl space-y-4">
        <ErrorAlert
          error={{
            status: undefined,
            error: "El análisis terminó en error",
            mensaje: "El proceso no pudo completar el análisis.",
            detalles: [],
          }}
          onRetry={cargar}
        />
        <Link to={`/procesos/${id}/analizar`} className="text-sm text-brand-600 hover:underline">
          Ir a reintentar el análisis
        </Link>
      </div>
    );
  }

  // Estado "Análisis completado".
  const fichaTecnica = resultado?.data?.fichaTecnica;
  const requisitosFinancieros = fichaTecnica?.requisitosFinancieros;
  const { valor: codigosRUPValor, pagina: codigosRUPPaginas } = normalizarValorConFuente(fichaTecnica?.codigosRUP);
  const codigosRUP = codigosRUPValor ?? [];
  const documentacion = resultado?.data?.documentacion ?? null;
  const perfiles = resultado?.data?.perfiles ?? [];
  const polizas = resultado?.data?.polizas ?? null;
  const polizasInfo = normalizarPolizas(polizas);

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Resultado del análisis</h1>
          <p className="mt-1 text-sm text-slate-500">
            Información extraída de los documentos por el flujo de análisis.
          </p>
        </div>
        <div className="flex gap-3">
          <Link to={`/procesos/${id}`}>
            <Button variant="secondary">Ver proceso</Button>
          </Link>
          <Button variant="primary" loading={descargando} onClick={handleDescargar}>
            Descargar Excel
          </Button>
        </div>
      </div>

      {errorDescarga && <ErrorAlert error={errorDescarga} />}

      {/* Ficha tecnica */}
      <section>
        <h2 className="mb-3 text-lg font-semibold text-slate-900">Ficha técnica</h2>
        {fichaTecnica ? (
          <div className="space-y-6">
            {/* Archivo analizado: mismo estilo que la seccion Documentacion requerida */}
            <p className="text-sm text-slate-600">
              <span className="font-medium text-slate-700">Archivo analizado:</span>{" "}
              {mostrarCampo(fichaTecnica.archivo).texto}
            </p>

            {/* Informacion general (incluye Indicadores financieros dentro de la misma tarjeta) */}
            <div>
              <h3 className="mb-2 text-sm font-semibold text-slate-700">Información general</h3>
              <div className="rounded-xl border border-slate-200 bg-white p-6">
                <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  {CAMPOS_FICHA.map(({ key, label, formatter }) => {
                    const { texto, pagina } = mostrarCampo(fichaTecnica[key], formatter);
                    return (
                      <div key={key}>
                        <div className="flex items-baseline justify-between gap-2">
                          <dt className="text-xs font-medium uppercase tracking-wide text-slate-400">{label}</dt>
                          <span className="whitespace-nowrap text-xs text-slate-400">{formatPages(pagina)}</span>
                        </div>
                        <dd className="mt-1 text-sm text-slate-800">{texto}</dd>
                      </div>
                    );
                  })}
                  <div className="sm:col-span-2">
                    <div className="flex items-baseline justify-between gap-2">
                      <dt className="text-xs font-medium uppercase tracking-wide text-slate-400">Códigos de RUP</dt>
                      <span className="whitespace-nowrap text-xs text-slate-400">{formatPages(codigosRUPPaginas)}</span>
                    </div>
                    <dd className="mt-1">
                      {codigosRUP.length > 0 ? (
                        <div className="flex flex-wrap gap-2">
                          {codigosRUP.map((codigo) => (
                            <Badge key={codigo} className="bg-brand-50 text-brand-700">
                              {codigo}
                            </Badge>
                          ))}
                        </div>
                      ) : (
                        <span className="text-sm text-slate-500">No disponible</span>
                      )}
                    </dd>
                  </div>
                </dl>

                {/* Indicadores financieros: subseccion dentro de la misma tarjeta de Informacion general */}
                <div className="mt-6 border-t border-slate-200 pt-6">
                  <h4 className="mb-3 text-sm font-semibold text-slate-700">Indicadores financieros</h4>
                  {requisitosFinancieros ? (
                    <dl className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-5">
                      {INDICADORES_FINANCIEROS.map(({ key, label, formatter }) => {
                        // Los indicadores son numeros "pelados" (no {valor,pagina}):
                        // la pagina esta al nivel de requisitosFinancieros, compartida
                        // por los 5 indicadores, no dentro de cada uno.
                        const { texto } = mostrarCampo(requisitosFinancieros[key], formatter);
                        return (
                          <div key={key}>
                            <div className="flex items-baseline justify-between gap-2">
                              <dt className="text-xs text-slate-500">{label}</dt>
                              <span className="whitespace-nowrap text-xs text-slate-400">{formatPages(requisitosFinancieros.pagina)}</span>
                            </div>
                            <dd className="mt-1 text-base font-semibold text-slate-800">{texto}</dd>
                          </div>
                        );
                      })}
                    </dl>
                  ) : (
                    <p className="text-sm text-slate-500">No disponible</p>
                  )}
                </div>
              </div>
            </div>
          </div>
        ) : (
          <p className="rounded-lg border border-dashed border-slate-300 bg-white py-6 text-center text-sm text-slate-500">
            El análisis no devolvió ficha técnica.
          </p>
        )}
      </section>

      {/* Documentacion */}
      <section>
        <h2 className="mb-3 text-lg font-semibold text-slate-900">Documentación requerida</h2>
        {documentacion ? (
          <div className="space-y-6">
            {/* Archivo analizado */}
            <p className="text-sm text-slate-600">
              <span className="font-medium text-slate-700">Archivo analizado:</span>{" "}
              {mostrarCampo(documentacion.nombreArchivo).texto}
            </p>

            {/* Capacidad Juridica */}
            <div>
              <h3 className="mb-2 text-sm font-semibold text-slate-700">Capacidad Jurídica</h3>
              <Table
                data={documentacion.capacidadJuridica ?? []}
                getRowKey={(row, index) => row.id ?? index}
                emptyMessage="No se encontraron requisitos de capacidad jurídica."
                columns={COLUMNAS_ID_DOCUMENTO}
              />
            </div>

            {/* Capacidad Tecnica: continua la numeracion de ID que envia el backend, no se reinicia en el frontend */}
            <div>
              <h3 className="mb-2 text-sm font-semibold text-slate-700">Capacidad Técnica</h3>
              <Table
                data={documentacion.capacidadTecnica ?? []}
                getRowKey={(row, index) => row.id ?? index}
                emptyMessage="No se encontraron requisitos de capacidad técnica."
                columns={COLUMNAS_ID_DOCUMENTO}
              />
            </div>

            {/* Criterios de Evaluacion: mismo item {id, documento, fuente} + puntos */}
            <div>
              <h3 className="mb-2 text-sm font-semibold text-slate-700">Criterios de Evaluación</h3>
              <Table
                data={documentacion.criteriosDeEvaluacion ?? []}
                getRowKey={(row, index) => row.id ?? index}
                emptyMessage="No se encontraron criterios de evaluación."
                columns={COLUMNAS_CRITERIOS_EVALUACION}
              />
            </div>

            {/* Recurso Humano Requerido: no tiene ID, se identifica por cargo */}
            <div>
              <h3 className="mb-2 text-sm font-semibold text-slate-700">Recurso Humano Requerido</h3>
              <Table
                data={documentacion.recursoHumanoRequerido ?? []}
                getRowKey={(row, index) => `${row.cargo ?? "recurso"}-${index}`}
                emptyMessage="No se encontró recurso humano requerido."
                columns={COLUMNAS_RECURSO_HUMANO}
              />
            </div>
          </div>
        ) : (
          <p className="rounded-lg border border-dashed border-slate-300 bg-white py-6 text-center text-sm text-slate-500">
            El análisis no devolvió documentación requerida.
          </p>
        )}
      </section>

      {/* Perfiles requeridos: cada grupo trae su propia fuente (archivo + pagina),
          no se repite por cargo; se muestra una tarjeta por grupo con sus cargos adentro. */}
      <section>
        <h2 className="mb-3 text-lg font-semibold text-slate-900">Perfiles requeridos</h2>
        {perfiles.length > 0 ? (
          <div className="space-y-6">
            {perfiles.map((grupo, grupoIndex) => (
              <div key={grupoIndex} className="rounded-xl border border-slate-200 bg-white p-6">
                <div className="mb-4 space-y-1 border-b border-slate-200 pb-4">
                  <p className="text-sm text-slate-700">
                    <span className="font-medium text-slate-800">Documento:</span>{" "}
                    {grupo.archivo ?? "No disponible"}
                  </p>
                  <p className="text-sm text-slate-700">{formatPages(grupo.pagina)}</p>
                </div>

                {grupo.cargos && grupo.cargos.length > 0 ? (
                  <div className="space-y-4">
                    {grupo.cargos.map((cargo, cargoIndex) => (
                      <div
                        key={cargoIndex}
                        className={cargoIndex > 0 ? "border-t border-slate-100 pt-4" : ""}
                      >
                        <h4 className="text-sm font-semibold text-slate-800">
                          {cargo.cargo ?? "No disponible"}
                        </h4>
                        <p className="mt-1 text-sm text-slate-600">
                          <span className="font-medium text-slate-700">Cantidad:</span>{" "}
                          {cargo.cantidad ?? "No disponible"}
                        </p>
                        <p className="mt-2 text-xs font-medium uppercase tracking-wide text-slate-400">
                          Perfil
                        </p>
                        <p className="mt-1 text-sm text-slate-800">{cargo.perfil ?? "No disponible"}</p>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-slate-500">No se encontraron cargos para este documento.</p>
                )}
              </div>
            ))}
          </div>
        ) : (
          <p className="rounded-lg border border-dashed border-slate-300 bg-white py-6 text-center text-sm text-slate-500">
            No se encontraron perfiles requeridos.
          </p>
        )}
      </section>

      {/* Polizas / Garantias: data.polizas trae 6 porcentajes "pelados"
          (numeros, no {valor,archivo,pagina}) mas UNA trazabilidad para todo
          el grupo (nombreArchivo/paginasConsultadas) — no hay pagina
          individual por poliza en el contrato actual. Ver normalizarPolizas. */}
      <section>
        <h2 className="mb-3 text-lg font-semibold text-slate-900">Pólizas / Garantías</h2>
        {polizas ? (
          <div className="rounded-xl border border-slate-200 bg-white p-6">
            {/* Trazabilidad del grupo completo, no por poliza individual */}
            <div className="mb-4 space-y-1 border-b border-slate-200 pb-4">
              <p className="text-sm text-slate-700">
                <span className="font-medium text-slate-800">Archivo fuente:</span>{" "}
                {polizasInfo.nombreArchivo ?? "No disponible"}
              </p>
              <p className="text-sm text-slate-700">
                <span className="font-medium text-slate-800">Páginas consultadas:</span>{" "}
                {polizasInfo.paginasConsultadas ? polizasInfo.paginasConsultadas.join(", ") : "No disponible"}
              </p>
            </div>

            <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {CAMPOS_POLIZAS.map(({ key, label }) => (
                <div
                  key={key}
                  className="flex items-baseline justify-between gap-2 rounded-lg border border-slate-100 p-4"
                >
                  <dt className="text-sm font-medium text-slate-700">{label}</dt>
                  <Badge className="shrink-0 bg-brand-50 text-brand-700">
                    {formatPolizaPorcentaje(polizasInfo.valores[key])}
                  </Badge>
                </div>
              ))}
            </dl>
          </div>
        ) : (
          <p className="rounded-lg border border-dashed border-slate-300 bg-white py-6 text-center text-sm text-slate-500">
            El análisis no devolvió pólizas / garantías.
          </p>
        )}
      </section>

      {/* Trazabilidad: modulo aun no implementado en n8n, seccion preparada sin logica */}
      <section>
        <h2 className="mb-3 text-lg font-semibold text-slate-900">Trazabilidad</h2>
        <p className="rounded-lg border border-dashed border-slate-300 bg-white py-6 text-center text-sm text-slate-500">
          Este módulo aún no está disponible. Se habilitará cuando el flujo de análisis de Trazabilidad de
          fuentes esté implementado en n8n.
        </p>
      </section>
    </div>
  );
}
