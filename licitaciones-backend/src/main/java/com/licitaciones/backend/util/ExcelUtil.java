package com.licitaciones.backend.util;

import lombok.experimental.UtilityClass;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Optional;

/**
 * Utilidades para rellenar una plantilla Excel (Apache POI) SIN alterar su
 * estructura: no crea hojas, filas ni estilos nuevos salvo cuando es
 * estrictamente necesario para agregar filas a una tabla (en cuyo caso
 * clona el estilo de la fila de referencia).
 */
@UtilityClass
public class ExcelUtil {

    /**
     * Busca, en las celdas de la hoja, una que contenga (sin distinguir
     * mayusculas/tildes basicas) la etiqueta indicada, y devuelve la celda
     * inmediatamente a su derecha (donde debe escribirse el valor). Esto
     * permite rellenar la plantilla sin depender de coordenadas fijas.
     *
     * Se usa "contiene" en vez de igualdad exacta porque algunas etiquetas
     * de la plantilla real traen saltos de linea o texto adicional (ej.
     * "Codigos de RUP\n(Clasificador de Bienes y Servicios)").
     */
    public static Optional<Cell> buscarCeldaValorPorEtiqueta(Sheet sheet, String etiqueta) {
        return buscarCeldaValorPorEtiqueta(sheet, etiqueta, 1);
    }

    /**
     * Igual que {@link #buscarCeldaValorPorEtiqueta(Sheet, String)}, pero
     * permite indicar cuantas columnas a la derecha de la etiqueta esta la
     * celda de valor. Necesario para tablas donde la columna inmediata a la
     * etiqueta no es la celda a diligenciar (ej. la tabla de indicadores
     * financieros de FichaTecnica: la columna inmediata es una formula fija
     * de la compania, y el valor requerido por el proceso va dos columnas
     * mas a la derecha).
     *
     * @param columnasDesplazamiento cuantas columnas despues de la etiqueta esta el valor (minimo 1).
     */
    public static Optional<Cell> buscarCeldaValorPorEtiqueta(Sheet sheet, String etiqueta, int columnasDesplazamiento) {
        String objetivo = normalizar(etiqueta);
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING && normalizar(cell.getStringCellValue()).contains(objetivo)) {
                    Cell valorCell = obtenerOCrearCelda(row, cell.getColumnIndex() + columnasDesplazamiento);
                    return Optional.of(valorCell);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Busca la primera fila que contenga una celda de texto cuyo contenido
     * incluya (sin distinguir mayusculas/minusculas) el texto indicado, y
     * devuelve su indice (0-based). Se usa para ubicar, dentro de una hoja,
     * un titulo o encabezado sin depender de coordenadas fijas (ej. el
     * titulo "DOCUMENTACIÓN REQUERIDA" de la hoja Documentacion).
     */
    public static Optional<Integer> buscarFilaPorTexto(Sheet sheet, String texto) {
        String objetivo = normalizar(texto);
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING && normalizar(cell.getStringCellValue()).contains(objetivo)) {
                    return Optional.of(row.getRowNum());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Busca, DENTRO de una unica fila de encabezado (no en toda la hoja), la
     * celda de texto cuyo contenido coincida EXACTAMENTE (normalizado: sin
     * espacios sobrantes, sin distinguir mayusculas/minusculas) con el
     * encabezado indicado, y devuelve su indice de columna (0-based).
     *
     * A diferencia de {@link #buscarCeldaValorPorEtiqueta}/{@link #buscarFilaPorTexto}
     * (que usan "contiene", pensados para titulos/etiquetas con texto
     * adicional como saltos de linea), esto usa IGUALDAD exacta: los
     * encabezados de una tabla suelen ser una palabra suelta ("ROL",
     * "Rol"), y una coincidencia parcial daria falsos positivos (ej. la
     * palabra "Control" contiene la subcadena "rol").
     */
    public static Optional<Integer> buscarColumnaPorEncabezado(Sheet sheet, int filaEncabezado, String encabezado) {
        Row fila = sheet.getRow(filaEncabezado);
        if (fila == null) {
            return Optional.empty();
        }
        String objetivo = normalizar(encabezado);
        for (Cell cell : fila) {
            if (cell.getCellType() == CellType.STRING && normalizar(cell.getStringCellValue()).equals(objetivo)) {
                return Optional.of(cell.getColumnIndex());
            }
        }
        return Optional.empty();
    }

    /** Ubicacion (fila, columna) 0-based de una celda de encabezado, ver {@link #buscarCeldaEncabezado}. */
    public record CeldaEncabezado(int fila, int columna) {
    }

    /**
     * Igual que {@link #buscarColumnaPorEncabezado}, pero busca en TODA la
     * hoja en vez de en una fila fija, y devuelve tambien la fila donde
     * aparecio. Necesario cuando el encabezado no esta necesariamente en la
     * primera fila de la hoja (ej. la hoja "Perfiles" trae un titulo
     * "Perfiles" en la fila 1 y recien el encabezado "Rol"/"Requisitos" en
     * la fila 2): asumir una fila fija de encabezado rompe la busqueda
     * aunque el texto coincida perfecto.
     */
    public static Optional<CeldaEncabezado> buscarCeldaEncabezado(Sheet sheet, String encabezado) {
        String objetivo = normalizar(encabezado);
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING && normalizar(cell.getStringCellValue()).equals(objetivo)) {
                    return Optional.of(new CeldaEncabezado(row.getRowNum(), cell.getColumnIndex()));
                }
            }
        }
        return Optional.empty();
    }

    /** Escribe un valor en la celda preservando el estilo ya definido en la plantilla. */
    public static void escribirValor(Cell cell, Object valor) {
        if (cell == null) {
            return;
        }
        if (valor == null) {
            cell.setBlank();
        } else if (valor instanceof Number number) {
            cell.setCellValue(number instanceof BigDecimal bd ? bd.doubleValue() : number.doubleValue());
        } else if (valor instanceof Boolean bool) {
            cell.setCellValue(bool ? "Si" : "No");
        } else {
            cell.setCellValue(String.valueOf(valor));
        }
    }

    /**
     * Agrega una fila de datos a una tabla que comienza en {@code filaEncabezado}
     * (fila con los titulos de columna), copiando el estilo de esa fila de
     * referencia para que la fila nueva respete el formato de la plantilla.
     * Escribe {@code valores} en las columnas 0, 1, 2... en ese orden; para
     * tablas donde las columnas de destino no son consecutivas desde la 0
     * (ej. localizadas dinamicamente con {@link #buscarColumnaPorEncabezado}),
     * usar {@link #escribirCelda} columna por columna en su lugar.
     *
     * @param filaDestino indice (0-based) de la fila donde se escribiran los valores.
     * @param filaEstilo  indice (0-based) de la fila de la que se copia el estilo (tipicamente el encabezado).
     */
    public static void escribirFila(Sheet sheet, int filaDestino, int filaEstilo, Object... valores) {
        for (int col = 0; col < valores.length; col++) {
            escribirCelda(sheet, filaDestino, col, filaEstilo, valores[col]);
        }
    }

    /**
     * Escribe un unico valor en la columna indicada de una fila de datos,
     * copiando el estilo de esa misma columna en {@code filaEstilo} cuando la
     * celda es nueva. Es la version "una columna arbitraria" de
     * {@link #escribirFila} (que asume columnas consecutivas desde la 0).
     *
     * @param filaDestino indice (0-based) de la fila donde se escribira el valor.
     * @param columna     indice (0-based) de la columna, tipicamente resuelto con {@link #buscarColumnaPorEncabezado}.
     * @param filaEstilo  indice (0-based) de la fila de la que se copia el estilo (tipicamente el encabezado).
     */
    public static void escribirCelda(Sheet sheet, int filaDestino, int columna, int filaEstilo, Object valor) {
        Row estilo = sheet.getRow(filaEstilo);
        Row destino = sheet.getRow(filaDestino);
        if (destino == null) {
            destino = sheet.createRow(filaDestino);
        }
        Cell celda = destino.getCell(columna);
        if (celda == null) {
            celda = destino.createCell(columna);
            if (estilo != null) {
                Cell celdaEstilo = estilo.getCell(columna);
                if (celdaEstilo != null) {
                    celda.setCellStyle(celdaEstilo.getCellStyle());
                }
            }
        }
        escribirValor(celda, valor);
    }

    /** Primera fila libre (sin contenido) por debajo de la fila de encabezado indicada. */
    public static int primeraFilaLibre(Sheet sheet, int filaEncabezado) {
        int fila = filaEncabezado + 1;
        while (sheet.getRow(fila) != null && filaConContenido(sheet.getRow(fila))) {
            fila++;
        }
        return fila;
    }

    private static boolean filaConContenido(Row row) {
        Iterator<Cell> it = row.cellIterator();
        while (it.hasNext()) {
            Cell cell = it.next();
            if (cell.getCellType() != CellType.BLANK) {
                return true;
            }
        }
        return false;
    }

    private static Cell obtenerOCrearCelda(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        return cell != null ? cell : row.createCell(columnIndex);
    }

    /**
     * Normaliza texto de encabezado/etiqueta para comparaciones tolerantes:
     * sin espacios al inicio/fin, sin distinguir mayusculas/minusculas, y con
     * espacios internos repetidos colapsados a uno solo (ej. "Rol",  "ROL" y
     * " rol " normalizan igual; "Índice   de Liquidez" y "Índice de Liquidez"
     * tambien).
     */
    private static String normalizar(String texto) {
        return texto == null ? "" : texto.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
