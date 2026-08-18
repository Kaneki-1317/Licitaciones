package com.licitaciones.backend.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ExcelUtil#buscarColumnaPorEncabezado} y {@link ExcelUtil#escribirCelda}:
 * localizar columnas de una tabla POR TEXTO de encabezado, sin asumir que
 * estan en un orden/indice fijo (necesario para la hoja "Perfiles", cuyas
 * columnas "ROL"/"REQUISITOS" pueden estar en cualquier posicion segun la
 * version de la plantilla).
 */
class ExcelUtilTest {

    @Test
    void buscarColumnaPorEncabezado_encuentraColumnaEnOrdenNormal() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Perfiles");
        Row encabezado = sheet.createRow(0);
        encabezado.createCell(0).setCellValue("ROL");
        encabezado.createCell(1).setCellValue("REQUISITOS");

        assertThat(ExcelUtil.buscarColumnaPorEncabezado(sheet, 0, "ROL")).contains(0);
        assertThat(ExcelUtil.buscarColumnaPorEncabezado(sheet, 0, "REQUISITOS")).contains(1);
    }

    @Test
    void buscarColumnaPorEncabezado_encuentraColumnaEnOrdenInvertidoYConColumnasExtra() {
        // Simula una plantilla real donde las columnas no empiezan en 0 ni
        // estan en el orden "esperado": REQUISITOS antes que ROL, con una
        // columna vacia/otra cosa en el medio.
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Perfiles");
        Row encabezado = sheet.createRow(0);
        encabezado.createCell(2).setCellValue("REQUISITOS");
        encabezado.createCell(5).setCellValue("ROL");

        assertThat(ExcelUtil.buscarColumnaPorEncabezado(sheet, 0, "ROL")).contains(5);
        assertThat(ExcelUtil.buscarColumnaPorEncabezado(sheet, 0, "REQUISITOS")).contains(2);
    }

    @Test
    void buscarColumnaPorEncabezado_esInsensibleAMayusculasYEspacios() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Perfiles");
        Row encabezado = sheet.createRow(0);
        encabezado.createCell(0).setCellValue("  rol  ");

        assertThat(ExcelUtil.buscarColumnaPorEncabezado(sheet, 0, "ROL")).contains(0);
    }

    @Test
    void buscarColumnaPorEncabezado_esInsensibleAEspaciosInternosRepetidos() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("FichaTecnica");
        Row encabezado = sheet.createRow(0);
        encabezado.createCell(0).setCellValue("Índice   de    Liquidez");

        assertThat(ExcelUtil.buscarColumnaPorEncabezado(sheet, 0, "Índice de Liquidez")).contains(0);
    }

    @Test
    void buscarColumnaPorEncabezado_noDaFalsoPositivoConSubcadena() {
        // "Control" contiene la subcadena "rol"; con igualdad exacta (a
        // diferencia de "contiene") esto NO debe matchear "ROL".
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Perfiles");
        Row encabezado = sheet.createRow(0);
        encabezado.createCell(0).setCellValue("Control");

        assertThat(ExcelUtil.buscarColumnaPorEncabezado(sheet, 0, "ROL")).isEmpty();
    }

    @Test
    void buscarColumnaPorEncabezado_devuelveVacioSiNoExisteElEncabezado() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Perfiles");
        Row encabezado = sheet.createRow(0);
        encabezado.createCell(0).setCellValue("OTRA COLUMNA");

        assertThat(ExcelUtil.buscarColumnaPorEncabezado(sheet, 0, "ROL")).isEmpty();
    }

    @Test
    void buscarColumnaPorEncabezado_devuelveVacioSiLaFilaDeEncabezadoNoExiste() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Perfiles");

        Optional<Integer> resultado = ExcelUtil.buscarColumnaPorEncabezado(sheet, 0, "ROL");

        assertThat(resultado).isEmpty();
    }

    @Test
    void escribirCelda_escribeEnColumnaArbitrariaSinTocarOtrasColumnas() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Perfiles");
        sheet.createRow(0).createCell(5).setCellValue("ROL");

        ExcelUtil.escribirCelda(sheet, 1, 5, 0, "Desarrolladores FullStack");

        Cell celda = sheet.getRow(1).getCell(5);
        assertThat(celda.getStringCellValue()).isEqualTo("Desarrolladores FullStack");
        // Ninguna otra columna de esa fila debe tener contenido.
        assertThat(sheet.getRow(1).getCell(0)).isNull();
        assertThat(sheet.getRow(1).getCell(1)).isNull();
    }

    @Test
    void escribirCelda_valorNuloQuedaEnBlancoSinRomper() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Perfiles");
        sheet.createRow(0).createCell(0).setCellValue("ROL");

        ExcelUtil.escribirCelda(sheet, 1, 0, 0, null);

        Cell celda = sheet.getRow(1).getCell(0);
        assertThat(celda.getCellType()).isEqualTo(CellType.BLANK);
    }

    @Test
    void buscarCeldaEncabezado_encuentraElEncabezadoAunqueNoEsteEnLaPrimeraFila() {
        // Reproduce EXACTAMENTE la estructura real de la plantilla: un
        // titulo "Perfiles" en la fila 1, y el encabezado "Rol"/"Requisitos"
        // recien en la fila 2. Buscar solo en la fila 0 (como hacia
        // buscarColumnaPorEncabezado) nunca lo hubiera encontrado.
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Perfiles");
        sheet.createRow(1).createCell(1).setCellValue("Perfiles");
        Row filaEncabezado = sheet.createRow(2);
        filaEncabezado.createCell(1).setCellValue("Rol");
        filaEncabezado.createCell(2).setCellValue("Requisitos");

        Optional<ExcelUtil.CeldaEncabezado> rol = ExcelUtil.buscarCeldaEncabezado(sheet, "ROL");
        Optional<ExcelUtil.CeldaEncabezado> requisitos = ExcelUtil.buscarCeldaEncabezado(sheet, "REQUISITOS");

        assertThat(rol).contains(new ExcelUtil.CeldaEncabezado(2, 1));
        assertThat(requisitos).contains(new ExcelUtil.CeldaEncabezado(2, 2));
    }

    @Test
    void buscarCeldaEncabezado_noEncuentraSiElTextoEstaMalEscrito() {
        // El caso real detectado en la plantilla: la celda dice literalmente
        // "Requicitos" (typo), no "Requisitos". buscarCeldaEncabezado no
        // debe inventar una coincidencia que no existe.
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Perfiles");
        Row fila = sheet.createRow(2);
        fila.createCell(1).setCellValue("Rol");
        fila.createCell(2).setCellValue("Requicitos");

        assertThat(ExcelUtil.buscarCeldaEncabezado(sheet, "REQUISITOS")).isEmpty();
        assertThat(ExcelUtil.buscarCeldaEncabezado(sheet, "ROL")).contains(new ExcelUtil.CeldaEncabezado(2, 1));
    }
}
