# Plantilla Excel — `plantilla_licitacion.xlsx`

`ExcelService` **nunca crea un libro desde cero**: siempre parte de este
archivo, conservando sus estilos, formulas, formatos y nombres de hoja, y
solo escribe los valores encontrados por n8n.

El archivo que viene en este repositorio es un **placeholder funcional**
(generado con `openpyxl`) que ya respeta la estructura que
`ExcelService` espera, para que el proyecto compile y se pueda probar
de inmediato. **Debes reemplazarlo por la plantilla oficial de la
entidad** en cuanto la tengas, manteniendo las mismas hojas/etiquetas
(o ajustando las constantes de `ExcelService` si la plantilla real usa
otros nombres).

## Estructura esperada

### Hoja `FichaTecnica`
Una celda de texto con cada etiqueta (columna A, por ejemplo), y el valor
se escribe en la celda inmediatamente a la derecha. La busqueda de la
etiqueta es case-insensitive, así que no depende de la fila/columna exacta:

| Etiqueta            | Se llena con                    |
|----------------------|----------------------------------|
| `Entidad`             | `fichaTecnica.entidad`          |
| `Numero de Proceso`   | `fichaTecnica.numeroProceso`    |
| `Objeto`              | `fichaTecnica.objeto`           |
| `Presupuesto`         | `fichaTecnica.presupuesto`      |
| `Plazo`               | `fichaTecnica.plazo`            |

### Hoja `Documentacion`
Fila 1 = encabezado (`Nombre del Documento`, `Obligatorio`). A partir de la
fila 2 se agrega una fila por cada elemento de `documentacion[]`, copiando
el estilo de la fila de encabezado.

### Hoja `Trazabilidad`
Fila 1 = encabezado (`Campo`, `Archivo`, `Pagina`). A partir de la fila 2
se agrega una fila por cada elemento de `fuentes[]`.

## Reemplazar por la plantilla oficial

1. Copia tu archivo oficial en esta carpeta con el nombre
   `plantilla_licitacion.xlsx` (sobrescribiendo el placeholder).
2. Si tus hojas/etiquetas tienen nombres distintos, ajusta las constantes
   `HOJA_FICHA_TECNICA`, `HOJA_DOCUMENTACION`, `HOJA_TRAZABILIDAD` y las
   etiquetas usadas en `ExcelService`.
3. No es necesario recompilar nada mas: el servicio lee el archivo desde
   el classpath en cada generacion.
