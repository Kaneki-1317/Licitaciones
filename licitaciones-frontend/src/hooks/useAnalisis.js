import { useCallback, useState } from "react";
import * as licitacionService from "../services/licitacionService";

/**
 * Orquesta el flujo de analisis de un proceso:
 *   1. POST /{id}/analizar (bloqueante, puede tardar varios minutos)
 *   2. si el proceso queda COMPLETADO, GET /{id}/resultado para traer
 *      fichaTecnica/documentacion/fuentes (el POST /analizar solo
 *      devuelve el ProcesoResponseDTO, no el detalle del analisis).
 *
 * @param {number|string} procesoId
 */
export function useAnalisis(procesoId) {
  const [fase, setFase] = useState("idle"); // idle | analizando | completado | error
  const [proceso, setProceso] = useState(null);
  const [resultado, setResultado] = useState(null);
  const [error, setError] = useState(null);

  const iniciarAnalisis = useCallback(
    async (archivos) => {
      setFase("analizando");
      setError(null);

      try {
        const procesoActualizado = await licitacionService.analizarDocumentos(procesoId, archivos);
        setProceso(procesoActualizado);

        if (procesoActualizado.estado !== "COMPLETADO") {
          // El backend solo responde 200 OK cuando el analisis termina
          // bien; si esto ocurre es un caso defensivo/inesperado.
          throw {
            status: 0,
            error: "Estado inesperado",
            mensaje: `El proceso quedo en estado ${procesoActualizado.estado} en lugar de COMPLETADO.`,
            detalles: [],
          };
        }

        const resultadoAnalisis = await licitacionService.consultarResultado(procesoId);
        setResultado(resultadoAnalisis);
        setFase("completado");
        return { proceso: procesoActualizado, resultado: resultadoAnalisis };
      } catch (err) {
        setError(err);
        setFase("error");
        throw err;
      }
    },
    [procesoId],
  );

  const reset = useCallback(() => {
    setFase("idle");
    setError(null);
  }, []);

  return { fase, proceso, resultado, error, iniciarAnalisis, reset };
}
