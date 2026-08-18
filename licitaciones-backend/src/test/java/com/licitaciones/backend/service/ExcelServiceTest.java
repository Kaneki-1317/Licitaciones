package com.licitaciones.backend.service;

import com.licitaciones.backend.config.ExcelProperties;
import com.licitaciones.backend.dto.response.AnalisisDataDTO;
import com.licitaciones.backend.dto.response.CargoDTO;
import com.licitaciones.backend.dto.response.N8nAnalisisResponseDTO;
import com.licitaciones.backend.dto.response.PerfilDTO;
import com.licitaciones.backend.dto.response.PolizaDTO;
import com.licitaciones.backend.dto.response.PolizasDTO;
import com.licitaciones.backend.util.ExcelUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Genera el Excel final contra la PLANTILLA REAL del proyecto
 * ({@code resources/templates/excel/PlantillaLicitaciones.xlsx}) y verifica
 * la hoja "Perfiles" con el ejemplo completo de 8 cargos del pedido,
 * repartidos en DOS bloques de {@code data.perfiles} (no solo perfiles[0]),
 * para confirmar:
 *
 * <ul>
 *   <li>cargo -&gt; ROL, perfil -&gt; REQUISITOS (columnas localizadas por
 *       texto de encabezado, no por indice fijo).</li>
 *   <li>se recorren TODOS los perfiles[] y TODOS sus cargos[], no solo el
 *       primer bloque.</li>
 *   <li>cantidad, pagina y archivo NUNCA aparecen en la hoja.</li>
 *   <li>encabezado y estilo de la plantilla no se tocan.</li>
 * </ul>
 *
 * La plantilla real, hoy, no trae una hoja "Perfiles" (confirmado
 * inspeccionando el .xlsx): este test ejercita por lo tanto el camino de
 * auto-creacion con encabezados "ROL"/"REQUISITOS" exactos.
 */
class ExcelServiceTest {

    @TempDir
    Path directorioTemporal;

    @Test
    void completarPerfiles_escribeRolYRequisitosDeTodosLosCargosDeTodosLosBloques() throws IOException {
        ExcelService servicio = new ExcelService(
                new ExcelProperties("templates/excel/PlantillaLicitaciones.xlsx", directorioTemporal.toString()));

        N8nAnalisisResponseDTO resultado = construirResultadoConDosBloquesDePerfiles();

        String rutaGenerada = servicio.generarExcel(999L, resultado);

        try (InputStream in = Files.newInputStream(Path.of(rutaGenerada));
             Workbook workbook = WorkbookFactory.create(in)) {

            Sheet hoja = workbook.getSheet("Perfiles");
            assertThat(hoja).as("la hoja Perfiles debe existir (auto-creada si la plantilla no la trae)").isNotNull();

            // Localiza el encabezado igual que la produccion: buscando en TODA
            // la hoja (no asumiendo fila 0), porque la plantilla real trae un
            // titulo "Perfiles" antes del encabezado "Rol"/"Requisitos".
            ExcelUtil.CeldaEncabezado celdaRol = ExcelUtil.buscarCeldaEncabezado(hoja, "ROL")
                    .orElseThrow(() -> new AssertionError("No se encontro el encabezado ROL en la hoja generada"));
            int columnaRol = celdaRol.columna();
            int columnaRequisitos = ExcelUtil.buscarCeldaEncabezado(hoja, "REQUISITOS")
                    .map(ExcelUtil.CeldaEncabezado::columna)
                    .orElse(columnaRol + 1);

            List<String[]> filas = leerFilasDeDatos(hoja, celdaRol.fila(), columnaRol, columnaRequisitos);

            assertThat(filas).hasSize(8);
            assertThat(filas.get(0)).containsExactly("Desarrolladores FullStack",
                    "Ingeniero de sistemas y/o tecnólogo en sistemas, Ingeniero electrónico o carreras afines a la NBC titulado con más de 2 años de experiencia profesional y experiencia en participación en proyectos de desarrollo de software.");
            assertThat(filas.get(1)[0]).isEqualTo("Líder User Experiencia - UX");
            assertThat(filas.get(2)[0]).isEqualTo("Ingeniero QA # Automatización");
            assertThat(filas.get(3)[0]).isEqualTo("Senior SME (Arquitecto)");
            // Segundo bloque de perfiles[] (archivo/pagina distintos): confirma que se recorren TODOS.
            assertThat(filas.get(4)[0]).isEqualTo("Ingeniero de Sistemas");
            assertThat(filas.get(5)[0]).isEqualTo("Gerente de proyectos");
            assertThat(filas.get(6)[0]).isEqualTo("Analista de Requisitos");
            assertThat(filas.get(7)[0]).isEqualTo("Ingeniero Especialista para la nube de Azure");

            // Ninguna fila de datos debe tener una tercera columna con contenido
            // (cantidad/pagina/archivo no se escriben en esta hoja).
            for (Row fila : hoja) {
                if (fila.getRowNum() <= celdaRol.fila()) {
                    continue; // titulo "Perfiles" y fila de encabezado "Rol"/"Requisitos"
                }
                for (Cell celda : fila) {
                    if (celda.getColumnIndex() != columnaRol && celda.getColumnIndex() != columnaRequisitos) {
                        assertThat(celda.getCellType())
                                .as("columna %d de la fila %d no deberia tener contenido", celda.getColumnIndex(), fila.getRowNum())
                                .isIn(CellType.BLANK, CellType._NONE);
                    }
                }
            }

            // Ningun valor de "cantidad" (16, 2, 4, 1, etc.) ni el nombre del
            // archivo aparecen en ninguna celda de la hoja.
            for (Row fila : hoja) {
                for (Cell celda : fila) {
                    if (celda.getCellType() == CellType.STRING) {
                        assertThat(celda.getStringCellValue()).doesNotContain("Anexo 1. Estudio Previo");
                    }
                }
            }
        }
    }

    /**
     * La hoja "Pólizas" de la plantilla real ya trae las seis garantias con
     * su celda de porcentaje en 0% (formato "0%"). Este test confirma que
     * generarExcel solo sobrescribe el VALOR de esas seis celdas (columna C,
     * filas 7 a 12) con la fraccion correspondiente al porcentaje recibido,
     * sin tocar su formato ni ninguna otra celda de la hoja (ej. las
     * formulas de "Vr. Asegurado" que dependen de esas mismas celdas).
     */
    @Test
    void completarPolizas_escribeSoloElPorcentajeDeCadaGarantiaSinTocarElFormato() throws IOException {
        ExcelService servicio = new ExcelService(
                new ExcelProperties("templates/excel/PlantillaLicitaciones.xlsx", directorioTemporal.toString()));

        AnalisisDataDTO data = new AnalisisDataDTO();
        PolizasDTO polizas = new PolizasDTO();
        polizas.setSeriedad(new PolizaDTO(10, "archivo.pdf", 5));
        polizas.setCumplimiento(new PolizaDTO(10, "archivo.pdf", 5));
        polizas.setBuenManejoAnticipo(new PolizaDTO(0, "archivo.pdf", 5));
        polizas.setPagoSalarios(new PolizaDTO(5, "archivo.pdf", 5));
        polizas.setEstabilidad(new PolizaDTO(0, "archivo.pdf", 5));
        polizas.setCalidad(new PolizaDTO(10, "archivo.pdf", 5));
        data.setPolizas(polizas);

        N8nAnalisisResponseDTO resultado = new N8nAnalisisResponseDTO();
        resultado.setSuccess(true);
        resultado.setData(data);

        String rutaGenerada = servicio.generarExcel(998L, resultado);

        try (InputStream in = Files.newInputStream(Path.of(rutaGenerada));
             Workbook workbook = WorkbookFactory.create(in)) {

            Sheet hoja = workbook.getSheet("Pólizas");
            assertThat(hoja).as("la plantilla real ya trae la hoja Pólizas").isNotNull();

            assertThat(hoja.getRow(6).getCell(1).getStringCellValue()).isEqualTo("Seriedad");
            assertThat(hoja.getRow(6).getCell(2).getNumericCellValue()).isEqualTo(0.10);
            assertThat(hoja.getRow(7).getCell(1).getStringCellValue()).isEqualTo("Cumplimiento");
            assertThat(hoja.getRow(7).getCell(2).getNumericCellValue()).isEqualTo(0.10);
            assertThat(hoja.getRow(8).getCell(1).getStringCellValue()).isEqualTo("Buen manejo del anticipo");
            assertThat(hoja.getRow(8).getCell(2).getNumericCellValue()).isEqualTo(0.0);
            assertThat(hoja.getRow(9).getCell(1).getStringCellValue()).isEqualTo("Pago de Salarios");
            assertThat(hoja.getRow(9).getCell(2).getNumericCellValue()).isEqualTo(0.05);
            assertThat(hoja.getRow(10).getCell(1).getStringCellValue()).isEqualTo("Estabilidad");
            assertThat(hoja.getRow(10).getCell(2).getNumericCellValue()).isEqualTo(0.0);
            assertThat(hoja.getRow(11).getCell(1).getStringCellValue()).isEqualTo("Calidad");
            assertThat(hoja.getRow(11).getCell(2).getNumericCellValue()).isEqualTo(0.10);

            // El formato de porcentaje de la plantilla se preserva tal cual (no se crea/reemplaza el estilo).
            for (int fila = 6; fila <= 11; fila++) {
                assertThat(hoja.getRow(fila).getCell(2).getCellStyle().getDataFormatString()).isEqualTo("0%");
            }

            // Las formulas que dependen de la columna Porcentaje no se tocan (siguen siendo formulas).
            assertThat(hoja.getRow(6).getCell(3).getCellType()).isEqualTo(CellType.FORMULA);
            assertThat(hoja.getRow(6).getCell(3).getCellFormula()).isEqualTo("+$B$4*C7");
        }
    }

    private List<String[]> leerFilasDeDatos(Sheet hoja, int filaEncabezado, int columnaRol, int columnaRequisitos) {
        List<String[]> filas = new ArrayList<>();
        Iterator<Row> it = hoja.rowIterator();
        while (it.hasNext()) {
            Row fila = it.next();
            if (fila.getRowNum() <= filaEncabezado) {
                continue; // titulo "Perfiles" y/o fila de encabezado "Rol"/"Requisitos"
            }
            Cell celdaRol = fila.getCell(columnaRol);
            if (celdaRol == null || celdaRol.getCellType() == CellType.BLANK) {
                continue;
            }
            Cell celdaReq = fila.getCell(columnaRequisitos);
            filas.add(new String[] {
                    celdaRol.getStringCellValue(),
                    celdaReq != null ? celdaReq.getStringCellValue() : ""
            });
        }
        return filas;
    }

    /** Los 8 cargos exactos del pedido, repartidos en DOS bloques de perfiles[] (no todos en perfiles[0]). */
    private N8nAnalisisResponseDTO construirResultadoConDosBloquesDePerfiles() {
        PerfilDTO bloque1 = new PerfilDTO();
        bloque1.setArchivo("Anexo 1. Estudio Previo 39118.pdf");
        bloque1.setPagina(16);
        bloque1.setCargos(List.of(
                cargo("Desarrolladores FullStack", 16,
                        "Ingeniero de sistemas y/o tecnólogo en sistemas, Ingeniero electrónico o carreras afines a la NBC titulado con más de 2 años de experiencia profesional y experiencia en participación en proyectos de desarrollo de software."),
                cargo("Líder User Experiencia - UX", 2,
                        "Profesional universitario en áreas afines al diseño gráfico, titulado con más de 2 años de experiencia interactuando con usuarios finales."),
                cargo("Ingeniero QA # Automatización", 4,
                        "Profesional en Sistemas, Ingeniero electrónico o carreras afines a la NBC con mínimo 2 años de experiencia profesional en la ejecución de pruebas de calidad en sistemas de información."),
                cargo("Senior SME (Arquitecto)", 2,
                        "Ingeniero de sistemas, electrónico o carreras afines a la NBC titulado con más de 5 años de experiencia profesional liderando en proyectos de desarrollo de software.")));

        PerfilDTO bloque2 = new PerfilDTO();
        bloque2.setArchivo("archivo2.pdf");
        bloque2.setPagina(20);
        bloque2.setCargos(List.of(
                cargo("Ingeniero de Sistemas", 2,
                        "Ingeniero de sistemas, electrónico o carreras afines a la NBC titulado con más de 2 años de experiencia en tecnologías CMS."),
                cargo("Gerente de proyectos", 1,
                        "Ingeniero de Sistemas, industrial, electrónico o carreras afines a la NBC, con Especialización en Gerencia de proyectos; con mínimo 5 años de experiencia profesional en la ejecución de proyectos en el área de tecnología."),
                cargo("Analista de Requisitos", 2,
                        "Ingeniero de sistemas y/o tecnólogo en sistemas, Ingeniero electrónico o carreras afines a la NBC titulado con más de 1 año de experiencia profesional."),
                cargo("Ingeniero Especialista para la nube de Azure", 1,
                        "Ingeniero de sistemas, electrónico o carreras afines a la NBC titulado con más de 5 años de experiencia profesional liderando en proyectos de desarrollo de software en nube de AZURE.")));

        AnalisisDataDTO data = new AnalisisDataDTO();
        data.setPerfiles(List.of(bloque1, bloque2));

        N8nAnalisisResponseDTO resultado = new N8nAnalisisResponseDTO();
        resultado.setSuccess(true);
        resultado.setData(data);
        return resultado;
    }

    private CargoDTO cargo(String cargo, int cantidad, String perfil) {
        CargoDTO dto = new CargoDTO();
        dto.setCargo(cargo);
        dto.setCantidad(cantidad);
        dto.setPerfil(perfil);
        return dto;
    }
}
