/**
 * Dispara la descarga de un Blob en el navegador, simulando un click en un
 * enlace temporal. Se usa para el Excel que devuelve
 * GET /api/licitaciones/{id}/excel.
 * @param {Blob} blob
 * @param {string} nombreArchivo
 */
export function triggerBlobDownload(blob, nombreArchivo) {
  const url = window.URL.createObjectURL(blob);
  const enlace = document.createElement("a");
  enlace.href = url;
  enlace.download = nombreArchivo;
  document.body.appendChild(enlace);
  enlace.click();
  enlace.remove();
  window.URL.revokeObjectURL(url);
}
