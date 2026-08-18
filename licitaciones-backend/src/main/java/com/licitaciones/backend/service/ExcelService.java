package com.licitaciones.backend.service;

import com.licitaciones.backend.config.ExcelProperties;
import com.licitaciones.backend.dto.response.AnalisisDataDTO;
import com.licitaciones.backend.dto.response.CampoTrazableDTO;
import com.licitaciones.backend.dto.response.CargoDTO;
import com.licitaciones.backend.dto.response.CodigosRupDTO;
import com.licitaciones.backend.dto.response.DocumentacionDTO;
import com.licitaciones.backend.dto.response.DocumentacionItemDTO;
import com.licitaciones.backend.dto.response.FichaTecnicaDTO;
import com.licitaciones.backend.dto.response.FuenteDTO;
import com.licitaciones.backend.dto.response.N8nAnalisisResponseDTO;
import com.licitaciones.backend.dto.response.PerfilDTO;
import com.licitaciones.backend.dto.response.PolizaDTO;
import com.licitaciones.backend.dto.response.PolizasDTO;
import com.licitaciones.backend.dto.response.RequisitosFinancierosDTO;
import com.licitaciones.backend.exception.ExcelGenerationException;
import com.licitaciones.backend.util.ExcelUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Genera el Excel final de un proceso de licitacion a partir de la
 * plantilla oficial ubicada en {@code resources/templates/excel}.
 *
 * IMPORTANTE: nunca se crea un libro desde cero. Siempre se parte de la
 * plantilla existente, conservando sus estilos, formulas, formatos y
 * nombres de hoja; este servicio solo escribe los valores encontrados.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelService {

    public static final String NOMBRE_ARCHIVO_FINAL = "Licitacion_Final.xlsx";

    private static final String HOJA_FICHA_TECNICA = "FichaTecnica";
    private static final String HOJA_DOCUMENTACION = "Documentacion";
    private static final String HOJA_TRAZABILIDAD = "Trazabilidad";
    private static final String HOJA_PERFILES = "Perfiles";
    private static final String HOJA_POLIZAS = "Pólizas";
    private static final String ENCABEZADO_ROL = "ROL";
    private static final String ENCABEZADO_REQUISITOS = "REQUISITOS";
    private static final String[] ENCABEZADOS_PERFILES = { ENCABEZADO_ROL, ENCABEZADO_REQUISITOS };
    private static final int FILA_ENCABEZADO_TABLA = 0;

    /**
     * Titulo fijo de la plantilla que marca el inicio del bloque de
     * documentacion en la hoja Documentacion (ver completarDocumentacion).
     * Se busca por texto, sin depender de la fila/columna exacta. El titulo
     * y el encabezado "ID"/"Documento" que la plantilla ya trae debajo de
     * el NUNCA se reescriben ni se duplican: la hoja usa unicamente esas
     * dos columnas, una sola vez, para todo el contenido.
     */
    private static final String TITULO_BLOQUE_DOCUMENTACION = "DOCUMENTACIÓN REQUERIDA";

    /**
     * En la tabla de indicadores financieros de FichaTecnica, la columna
     * inmediata a la etiqueta (offset 1) es una formula fija con el
     * indicador propio de la compania (hoja IndicadoresSG) y NO debe
     * sobrescribirse. El valor que exige el proceso (lo que entrega
     * requisitosFinancieros de n8n) va en la columna "REQUERIDO", dos
     * columnas a la derecha de la etiqueta.
     */
    private static final int OFFSET_COLUMNA_INDICADOR_REQUERIDO = 2;

    private final ExcelProperties excelProperties;

    /**
     * Diligencia la plantilla con el resultado del analisis y guarda el
     * archivo final en disco.
     *
     * @return ruta absoluta del archivo Excel generado.
     */
    public String generarExcel(Long procesoId, N8nAnalisisResponseDTO resultado) {
        try (InputStream plantilla = new ClassPathResource(excelProperties.templatePath()).getInputStream();
             Workbook workbook = WorkbookFactory.create(plantilla)) {

            // Hay formulas en la plantilla que dependen de los valores que
            // escribimos (ej. "Presupuesto en SMMLV" = Presupuesto / SMMLV).
            // Forzamos a Excel a recalcularlas al abrir el archivo, en vez
            // de mostrar el valor cacheado de la plantilla original.
            workbook.setForceFormulaRecalculation(true);

            AnalisisDataDTO datos = resultado.getData();
            completarFichaTecnica(workbook, datos != null ? datos.getFichaTecnica() : null);
            completarDocumentacion(workbook, datos != null ? datos.getDocumentacion() : null,
                    datos != null ? datos.getPerfiles() : null);
            completarTrazabilidad(workbook, datos != null ? datos.getFuentes() : null);
            completarPerfiles(workbook, datos != null ? datos.getPerfiles() : null);
            completarPolizas(workbook, datos != null ? datos.getPolizas() : null);

            Path archivoSalida = resolverRutaSalida(procesoId);
            Files.createDirectories(archivoSalida.getParent());

            try (OutputStream out = Files.newOutputStream(archivoSalida)) {
                workbook.write(out);
            }

            log.info("Excel generado para el proceso {} en {}", procesoId, archivoSalida);
            return archivoSalida.toAbsolutePath().toString();

        } catch (IOException ex) {
            throw new ExcelGenerationException(
                    "No fue posible generar el Excel del proceso " + procesoId + ": " + ex.getMessage(), ex);
        }
    }

    private void completarFichaTecnica(Workbook workbook, FichaTecnicaDTO ficha) {
        if (ficha == null) {
            return;
        }
        Sheet sheet = obtenerHoja(workbook, HOJA_FICHA_TECNICA);

        // Etiquetas identicas a las de la plantilla real (hoja FichaTecnica,
        // columna A). "Nombre Proyecto", "Tipo de Proceso" y "Modalidad de
        // ejecucion" no se escriben: aunque n8n ya envia nombreProyecto, el
        // Excel todavia no tiene una etiqueta para el (fuera del alcance de
        // este arreglo). "Presupuesto en SMMLV" tampoco: es una formula de
        // la propia plantilla que se recalcula sola a partir de "Presupuesto".
        //
        // Cada campo de fichaTecnica llega envuelto en CampoTrazableDTO
        // ({valor, pagina}); el Excel solo necesita "valor" (la pagina es
        // para la trazabilidad que muestra el frontend), de ahi el
        // valorDe(...) en cada llamada.
        escribirEtiqueta(sheet, "Entidad", valorDe(ficha.getEntidad()));
        escribirEtiqueta(sheet, "Objeto del Contrato / Alcance", valorDe(ficha.getObjetoContrato()));
        escribirEtiqueta(sheet, "Presupuesto", valorDe(ficha.getPresupuesto()));
        escribirEtiqueta(sheet, "Tiempo de Ejecución", valorDe(ficha.getPlazoProyecto()));
        escribirEtiqueta(sheet, "Ciudad del proyecto", valorDe(ficha.getCiudad()));
        escribirEtiqueta(sheet, "Valoración", valorDe(ficha.getValoracion()));
        escribirEtiqueta(sheet, "Códigos de RUP", formatearCodigosRUP(ficha.getCodigosRUP()));

        completarIndicadoresFinancieros(sheet, ficha.getRequisitosFinancieros());
    }

    /** Extrae el "valor" de un CampoTrazableDTO, o null si el campo completo no vino en la respuesta. */
    private <T> T valorDe(CampoTrazableDTO<T> campo) {
        return campo == null ? null : campo.getValor();
    }

    /**
     * Diligencia la columna "REQUERIDO" de la tabla de indicadores
     * financieros (no la columna inmediata a la etiqueta, que es una
     * formula fija de la compania que no debe tocarse).
     */
    private void completarIndicadoresFinancieros(Sheet sheet, RequisitosFinancierosDTO indicadores) {
        if (indicadores == null) {
            return;
        }
        escribirIndicadorRequerido(sheet, "Índice de Liquidez", indicadores.getIndiceLiquidez());
        escribirIndicadorRequerido(sheet, "Índice de Endeudamiento", indicadores.getNivelEndeudamiento());
        escribirIndicadorRequerido(sheet, "Razón de Cobertura a intereses", indicadores.getCoberturaIntereses());
        escribirIndicadorRequerido(sheet, "Rentabilidad del patrimonio", indicadores.getRoe());
        escribirIndicadorRequerido(sheet, "Rentabilidad del activo", indicadores.getRoa());
    }

    private void escribirEtiqueta(Sheet sheet, String etiqueta, Object valor) {
        if (valor == null) {
            return;
        }
        ExcelUtil.buscarCeldaValorPorEtiqueta(sheet, etiqueta)
                .ifPresent(cell -> ExcelUtil.escribirValor(cell, valor));
    }

    private void escribirIndicadorRequerido(Sheet sheet, String etiqueta, Object valor) {
        if (valor == null) {
            return;
        }
        ExcelUtil.buscarCeldaValorPorEtiqueta(sheet, etiqueta, OFFSET_COLUMNA_INDICADOR_REQUERIDO)
                .ifPresent(cell -> ExcelUtil.escribirValor(cell, valor));
    }

    private String formatearCodigosRUP(CodigosRupDTO codigosRUP) {
        if (codigosRUP == null || codigosRUP.getValor() == null || codigosRUP.getValor().isEmpty()) {
            return null;
        }
        return codigosRUP.getValor().stream().map(String::valueOf).collect(Collectors.joining(", "));
    }

    /**
     * documentacion paso de ser un array plano ({@code {nombre, obligatorio}})
     * a un objeto con 4 categorias propias (ver {@link DocumentacionDTO}). La
     * hoja "Documentacion" de la plantilla oficial solo tiene DOS columnas
     * utilizables, "ID" y "Documento" (debajo del titulo fijo
     * "DOCUMENTACIÓN REQUERIDA"): no hay una tercera columna para
     * categoria/observaciones/puntos, y ese titulo + encabezado son unicos
     * para toda la hoja, no se repiten. Por eso cada categoria se escribe
     * como una fila propia dentro de esas mismas dos columnas (ID = nombre
     * de la categoria, Documento en blanco), seguida de sus items.
     *
     * capacidadJuridica, capacidadTecnica y criteriosDeEvaluacion comparten
     * una numeracion de ID unica y consecutiva (no se reinicia entre ellas:
     * si juridica termina en 16, tecnica sigue en 17).
     *
     * RECURSO HUMANO REQUERIDO NO usa documentacion.recursoHumanoRequerido:
     * esa lista es una extraccion aparte que el frontend todavia muestra,
     * pero seria una segunda extraccion del mismo recurso humano si tambien
     * se usara aqui. La unica fuente para esta seccion es data.perfiles (la
     * misma que llena la hoja Perfiles): se aplanan sus cargos y, por cada
     * uno, la columna ID muestra "{cantidad} {cargo}" y la columna Documento
     * el perfil completo, sin resumir ni modificar.
     *
     * Cada categoria con elementos queda separada de la siguiente por una
     * fila en blanco; las categorias sin elementos no se escriben.
     */
    private void completarDocumentacion(Workbook workbook, DocumentacionDTO documentacion, List<PerfilDTO> perfiles) {
        if (documentacion == null) {
            return;
        }
        Sheet sheet = obtenerHoja(workbook, HOJA_DOCUMENTACION);
        escribirEtiqueta(sheet, "Nombre del Archivo", documentacion.getNombreArchivo());

        Optional<Integer> filaTitulo = ExcelUtil.buscarFilaPorTexto(sheet, TITULO_BLOQUE_DOCUMENTACION);
        if (filaTitulo.isEmpty()) {
            log.warn("La hoja {} no tiene el titulo '{}' esperado; no se escribe la tabla de documentos.",
                    HOJA_DOCUMENTACION, TITULO_BLOQUE_DOCUMENTACION);
            return;
        }
        // El titulo y el encabezado ID/Documento de la plantilla no se tocan; los datos empiezan justo debajo,
        // reutilizando las filas en blanco que la plantilla ya trae preformateadas.
        int filaDatos = filaTitulo.get() + 2;

        List<Object[]> juridica = filasNumeradas(documentacion.getCapacidadJuridica(), 1);
        List<Object[]> tecnica = filasNumeradas(documentacion.getCapacidadTecnica(), 1 + juridica.size());
        List<Object[]> criterios = filasNumeradas(documentacion.getCriteriosDeEvaluacion(), 1 + juridica.size() + tecnica.size());
        List<Object[]> recursoHumano = filasDeRecursoHumano(aplanarCargos(perfiles));

        List<SeccionDocumentacion> secciones = List.of(
                new SeccionDocumentacion("Capacidad Juridica", juridica),
                new SeccionDocumentacion("Capacidad Tecnica", tecnica),
                new SeccionDocumentacion("CRITERIOS DE EVALUACIÓN", criterios),
                new SeccionDocumentacion("RECURSO HUMANO REQUERIDO", recursoHumano));

        // Solo se escribe con ExcelUtil.escribirFila: si la celda ya existe (las ~94 filas en blanco
        // que trae la plantilla debajo del encabezado) reutiliza su estilo tal cual, sin tocarlo; solo
        // clona el formato de filaDatos cuando hace falta una fila nueva porque los datos superan el
        // espacio ya preformateado por la plantilla.
        int fila = filaDatos;
        boolean escritaAlguna = false;
        for (SeccionDocumentacion seccion : secciones) {
            if (seccion.filas().isEmpty()) {
                continue;
            }
            if (escritaAlguna) {
                fila++; // fila en blanco entre secciones (ya existe en la plantilla; se deja tal cual)
            }
            ExcelUtil.escribirFila(sheet, fila++, filaDatos, seccion.nombre());
            for (Object[] valores : seccion.filas()) {
                ExcelUtil.escribirFila(sheet, fila++, filaDatos, valores[0], valores[1]);
            }
            escritaAlguna = true;
        }
    }

    private List<Object[]> filasNumeradas(List<DocumentacionItemDTO> items, int idInicial) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<Object[]> filas = new ArrayList<>();
        int id = idInicial;
        for (DocumentacionItemDTO item : items) {
            filas.add(new Object[] { id++, item.getDocumento() });
        }
        return filas;
    }

    /** RECURSO HUMANO REQUERIDO reutiliza los cargos ya aplanados de data.perfiles (ver aplanarCargos). */
    private List<Object[]> filasDeRecursoHumano(List<CargoDTO> cargos) {
        if (cargos == null || cargos.isEmpty()) {
            return List.of();
        }
        List<Object[]> filas = new ArrayList<>();
        for (CargoDTO cargo : cargos) {
            String id = (valorTexto(cargo.getCantidad()) + " " + valorTexto(cargo.getCargo())).trim();
            filas.add(new Object[] { id, cargo.getPerfil() });
        }
        return filas;
    }

    /**
     * Aplana data.perfiles (grupos por archivo/pagina) en la lista plana de
     * cargos que consumen tanto la hoja Perfiles como la seccion RECURSO
     * HUMANO REQUERIDO de Documentacion: una unica extraccion, dos usos.
     */
    private List<CargoDTO> aplanarCargos(List<PerfilDTO> perfiles) {
        return perfiles == null
                ? List.of()
                : perfiles.stream()
                        .filter(grupo -> grupo.getCargos() != null)
                        .flatMap(grupo -> grupo.getCargos().stream())
                        .toList();
    }

    private String valorTexto(Object valor) {
        return valor == null ? "" : String.valueOf(valor).trim();
    }

    /** Una seccion de la hoja Documentacion: su nombre (fila ID, Documento en blanco) y las filas [id, documento] debajo. */
    private record SeccionDocumentacion(String nombre, List<Object[]> filas) {
    }

    private void completarTrazabilidad(Workbook workbook, java.util.List<FuenteDTO> fuentes) {
        if (fuentes == null || fuentes.isEmpty()) {
            return;
        }
        Sheet sheet = obtenerHoja(workbook, HOJA_TRAZABILIDAD);
        int fila = ExcelUtil.primeraFilaLibre(sheet, FILA_ENCABEZADO_TABLA);
        for (FuenteDTO fuente : fuentes) {
            ExcelUtil.escribirFila(sheet, fila++, FILA_ENCABEZADO_TABLA,
                    fuente.getCampo(), fuente.getArchivo(), fuente.getPagina());
        }
    }

    /**
     * Si la plantilla ya trae la hoja "Perfiles", se reutiliza tal cual, sin
     * tocar su titulo, formato ni encabezados: el encabezado "ROL"/"REQUISITOS"
     * se localiza dinamicamente buscando en TODA la hoja (no en una fila
     * fija), porque la plantilla real trae un titulo "Perfiles" en la
     * primera fila y el encabezado recien en la siguiente (ver
     * {@link ExcelUtil#buscarCeldaEncabezado}). Si la hoja no existe
     * todavia, se crea automaticamente con exactamente esas dos columnas en
     * la fila 0 (ver {@link #obtenerOCrearHojaPerfiles}).
     *
     * "REQUISITOS" tiene ademas un respaldo: si no se encuentra ese texto
     * exacto (la plantilla real actual lo trae escrito "Requicitos"), se usa
     * la columna inmediatamente a la derecha de "ROL" y se deja constancia
     * en el log — no se asume en silencio, y no se falla solo por una
     * diferencia de tipeo que no afecta la ubicacion real del dato.
     *
     * Unicamente se escriben "ROL" (= cargo.cargo) y "REQUISITOS"
     * (= cargo.perfil, copiado completo, sin resumir): archivo, pagina y
     * cantidad son metadata/trazabilidad de data.perfiles que esta hoja no
     * debe mostrar (van a la seccion "Perfiles requeridos" del frontend).
     *
     * Los mismos cargos aplanados aqui (ver aplanarCargos) son la unica
     * fuente de la seccion RECURSO HUMANO REQUERIDO en completarDocumentacion:
     * data.perfiles se extrae una sola vez y se reutiliza en ambos lugares.
     */
    private void completarPerfiles(Workbook workbook, List<PerfilDTO> perfiles) {
        Sheet sheet = obtenerOCrearHojaPerfiles(workbook);

        ExcelUtil.CeldaEncabezado celdaRol = ExcelUtil.buscarCeldaEncabezado(sheet, ENCABEZADO_ROL)
                .orElseThrow(() -> new ExcelGenerationException(
                        "La hoja '" + HOJA_PERFILES + "' no tiene el encabezado esperado: '" + ENCABEZADO_ROL + "'"));
        int filaEncabezado = celdaRol.fila();
        int columnaRol = celdaRol.columna();

        int columnaRequisitos = ExcelUtil.buscarCeldaEncabezado(sheet, ENCABEZADO_REQUISITOS)
                .map(ExcelUtil.CeldaEncabezado::columna)
                .orElseGet(() -> {
                    int columnaAdyacente = columnaRol + 1;
                    log.warn("La hoja '{}' no tiene un encabezado con el texto exacto '{}'; se usa la columna "
                                    + "inmediatamente a la derecha de '{}' (columna {} de la fila {}) en su lugar. "
                                    + "Revisa el texto de ese encabezado en la plantilla si esto no es correcto.",
                            HOJA_PERFILES, ENCABEZADO_REQUISITOS, ENCABEZADO_ROL, columnaAdyacente, filaEncabezado);
                    return columnaAdyacente;
                });

        List<CargoDTO> cargos = aplanarCargos(perfiles);

        int fila = ExcelUtil.primeraFilaLibre(sheet, filaEncabezado);
        for (CargoDTO cargo : cargos) {
            ExcelUtil.escribirCelda(sheet, fila, columnaRol, filaEncabezado, cargo.getCargo());
            ExcelUtil.escribirCelda(sheet, fila, columnaRequisitos, filaEncabezado, cargo.getPerfil());
            fila++;
        }
    }

    private Sheet obtenerOCrearHojaPerfiles(Workbook workbook) {
        Sheet existente = workbook.getSheet(HOJA_PERFILES);
        if (existente != null) {
            return existente;
        }

        Sheet nueva = workbook.createSheet(HOJA_PERFILES);
        Font fuenteEncabezado = workbook.createFont();
        fuenteEncabezado.setBold(true);
        CellStyle estiloEncabezado = workbook.createCellStyle();
        estiloEncabezado.setFont(fuenteEncabezado);

        Row encabezado = nueva.createRow(FILA_ENCABEZADO_TABLA);
        for (int col = 0; col < ENCABEZADOS_PERFILES.length; col++) {
            Cell celda = encabezado.createCell(col);
            celda.setCellValue(ENCABEZADOS_PERFILES[col]);
            celda.setCellStyle(estiloEncabezado);
        }
        return nueva;
    }

    /**
     * Diligencia UNICAMENTE la columna "Porcentaje" de la hoja "Pólizas": la
     * plantilla ya trae las seis garantias (Seriedad, Cumplimiento, Buen
     * manejo del anticipo, Pago de Salarios, Estabilidad, Calidad), cada una
     * con su celda de porcentaje ya formateada como "0%". Aqui solo se
     * localiza esa celda por el nombre de la garantia (columna B, buscado
     * con {@link ExcelUtil#buscarCeldaValorPorEtiqueta}) y se sobrescribe su
     * valor numerico en la columna inmediata (columna C, "Porcentaje"): no
     * se toca el estilo/formato de la celda (ya trae "0%" en la plantilla),
     * ni se crean filas, columnas ni hojas nuevas. Las formulas de la
     * plantilla que dependen de esta columna (Vr. Asegurado, Prima, etc.)
     * se recalculan solas gracias a setForceFormulaRecalculation.
     *
     * Si una garantia no viene en la respuesta (poliza == null o su valor es
     * null), su celda no se toca y conserva el "0%" que ya trae la
     * plantilla.
     */
    private void completarPolizas(Workbook workbook, PolizasDTO polizas) {
        if (polizas == null) {
            return;
        }
        Sheet sheet = obtenerHoja(workbook, HOJA_POLIZAS);
        escribirPorcentajePoliza(sheet, "Seriedad", valorPoliza(polizas.getSeriedad()));
        escribirPorcentajePoliza(sheet, "Cumplimiento", valorPoliza(polizas.getCumplimiento()));
        escribirPorcentajePoliza(sheet, "Buen manejo del anticipo", valorPoliza(polizas.getBuenManejoAnticipo()));
        escribirPorcentajePoliza(sheet, "Pago de Salarios", valorPoliza(polizas.getPagoSalarios()));
        escribirPorcentajePoliza(sheet, "Estabilidad", valorPoliza(polizas.getEstabilidad()));
        escribirPorcentajePoliza(sheet, "Calidad", valorPoliza(polizas.getCalidad()));
    }

    private Integer valorPoliza(PolizaDTO poliza) {
        return poliza == null ? null : poliza.getValor();
    }

    /**
     * Escribe el porcentaje (ej. 10 -> 10%) en la celda de la garantia
     * indicada, preservando el formato de porcentaje que la celda ya trae en
     * la plantilla. La celda ya esta formateada como "0%", por lo que Excel
     * espera la fraccion equivalente (10% = 0.10), de ahi la division entre
     * 100; el estilo de la celda no se modifica, solo su valor.
     */
    private void escribirPorcentajePoliza(Sheet sheet, String etiqueta, Integer porcentaje) {
        if (porcentaje == null) {
            return;
        }
        ExcelUtil.buscarCeldaValorPorEtiqueta(sheet, etiqueta)
                .ifPresent(cell -> ExcelUtil.escribirValor(cell, porcentaje / 100.0));
    }

    private Sheet obtenerHoja(Workbook workbook, String nombre) {
        Sheet sheet = workbook.getSheet(nombre);
        if (sheet == null) {
            throw new ExcelGenerationException(
                    "La plantilla de Excel no contiene la hoja esperada: '" + nombre + "'");
        }
        return sheet;
    }

    private Path resolverRutaSalida(Long procesoId) {
        return Path.of(excelProperties.outputDir(), String.valueOf(procesoId), NOMBRE_ARCHIVO_FINAL);
    }
}
