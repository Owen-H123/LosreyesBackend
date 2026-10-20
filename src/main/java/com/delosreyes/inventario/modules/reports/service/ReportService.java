package com.delosreyes.inventario.modules.reports.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delosreyes.inventario.modules.products.repository.ProductRepository;
import com.delosreyes.inventario.modules.reports.dto.ReportDtos;
import com.delosreyes.inventario.modules.sales.repository.SaleItemRepository;
import com.delosreyes.inventario.modules.sales.repository.SaleRepository;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * ReportService — Generación de reportes en JSON, Excel y PDF.
 *
 * QUÉ HACE:
 *   1. Consulta datos analíticos desde SaleRepository y ProductRepository.
 *   2. Construye objetos de reporte con totales y métricas.
 *   3. Exporta a Excel (.xlsx) usando Apache POI.
 *   4. Exporta a PDF usando iText.
 *
 * REPORTES DISPONIBLES:
 *   - dailySalesReport:    Ventas agrupadas por día en un rango de fechas
 *   - topProductsReport:   Productos más vendidos por cantidad y revenue
 *   - lowStockReport:      Alertas de productos con stock por debajo del mínimo
 *
 * ACCESO: Solo ADMIN (configurado en ReportController con @PreAuthorize).
 *
 * RELACIONES:
 *   ReportController → ReportService → SaleRepository, SaleItemRepository, ProductRepository
 *
 * ESTILO DE EXCEL:
 *   Los estilos de las hojas (título, encabezado, moneda, filas alternadas,
 *   total) se arman con métodos privados reutilizables (ver sección
 *   "ESTILOS EXCEL" al final de la clase) para que ambos reportes
 *   (Ventas Diarias y Stock Bajo) mantengan la misma identidad visual.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    // Color de marca — azul oscuro usado en encabezados y título
    private static final byte[] BRAND_DARK_BLUE = new byte[]{(byte) 15, (byte) 35, (byte) 80};
    // Fondo alterno (zebra) para filas de datos
    private static final byte[] ZEBRA_GRAY = new byte[]{(byte) 235, (byte) 238, (byte) 245};
    // Fondo de la fila de totales
    private static final byte[] TOTAL_GOLD = new byte[]{(byte) 255, (byte) 230, (byte) 179};

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;

    public ReportService(SaleRepository saleRepository,
                         SaleItemRepository saleItemRepository,
                         ProductRepository productRepository) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.productRepository = productRepository;
    }

    // ============================================================
    // REPORTE: VENTAS DIARIAS
    // ============================================================
    public ReportDtos.DailySalesReport getDailySalesReport(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt   = to.atTime(LocalTime.MAX);

        List<Object[]> rows = saleRepository.findDailyRevenue(fromDt, toDt);

        List<ReportDtos.DailySalesRow> reportRows = rows.stream().map(row -> {
            ReportDtos.DailySalesRow r = new ReportDtos.DailySalesRow();
            r.setDay(((java.sql.Date) row[0]).toLocalDate());
            r.setCount(((Number) row[1]).longValue());
            r.setRevenue(new BigDecimal(row[2].toString()));
            r.setNetRevenue(new BigDecimal(row[3].toString()));
            return r;
        }).collect(Collectors.toList());

        BigDecimal totalRevenue = reportRows.stream()
            .map(ReportDtos.DailySalesRow::getRevenue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalSales = reportRows.stream()
            .mapToLong(ReportDtos.DailySalesRow::getCount).sum();

        ReportDtos.DailySalesReport report = new ReportDtos.DailySalesReport();
        report.setFrom(from);
        report.setTo(to);
        report.setTotalRevenue(totalRevenue);
        report.setTotalSales(totalSales);
        report.setRows(reportRows);
        return report;
    }

    // ============================================================
    // REPORTE: TOP PRODUCTOS
    // ============================================================
    public List<ReportDtos.TopProductRow> getTopProductsReport(LocalDate from, LocalDate to, int limit) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt   = to.atTime(LocalTime.MAX);

        return saleItemRepository.findTopProducts(fromDt, toDt, limit).stream().map(row -> {
            ReportDtos.TopProductRow r = new ReportDtos.TopProductRow();
            r.setProductId(((Number) row[0]).longValue());
            r.setCode((String) row[1]);
            r.setName((String) row[2]);
            r.setTotalQty(((Number) row[3]).longValue());
            r.setTotalRevenue(new BigDecimal(row[4].toString()));
            return r;
        }).collect(Collectors.toList());
    }

    // ============================================================
    // REPORTE: STOCK BAJO
    // ============================================================
    public List<ReportDtos.LowStockRow> getLowStockReport() {
        return productRepository.findLowStock().stream().map(p -> {
            ReportDtos.LowStockRow r = new ReportDtos.LowStockRow();
            r.setProductId(p.getId());
            r.setCode(p.getCode());
            r.setName(p.getName());
            r.setCategory(p.getCategory().getName());
            r.setCurrentStock(p.getStock());
            r.setMinimumStock(p.getMinimumStock());
            r.setShortage(p.getMinimumStock() - p.getStock());
            return r;
        }).collect(Collectors.toList());
    }

    // ============================================================
    // EXPORTAR EXCEL: Ventas diarias
    // ============================================================
    public byte[] exportDailySalesExcel(LocalDate from, LocalDate to) throws Exception {
        ReportDtos.DailySalesReport report = getDailySalesReport(from, to);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Ventas Diarias");
            sheet.setDisplayGridlines(false);

            int colCount = 4;
            String[] headers = {"Fecha", "N° Ventas", "Ingresos (S/)", "Ingresos Netos (S/)"};

            // Estilos reutilizables
            CellStyle titleStyle    = buildTitleStyle(workbook);
            CellStyle subtitleStyle = buildSubtitleStyle(workbook);
            CellStyle headerStyle   = buildHeaderStyle(workbook);
            CellStyle textStyle     = buildDataStyle(workbook, false, false);
            CellStyle textStyleAlt  = buildDataStyle(workbook, true, false);
            CellStyle moneyStyle    = buildDataStyle(workbook, false, true);
            CellStyle moneyStyleAlt = buildDataStyle(workbook, true, true);
            CellStyle totalLabelStyle = buildTotalStyle(workbook, false);
            CellStyle totalMoneyStyle = buildTotalStyle(workbook, true);

            // Título
            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(26);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Reporte de Ventas Diarias — De los Reyes");
            titleCell.setCellStyle(titleStyle);
            for (int i = 1; i < colCount; i++) {
                Cell c = titleRow.createCell(i);
                c.setCellStyle(titleStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, colCount - 1));

            // Subtítulo con el periodo
            Row subRow = sheet.createRow(1);
            Cell subCell = subRow.createCell(0);
            subCell.setCellValue("Periodo: " + from + "  al  " + to);
            subCell.setCellStyle(subtitleStyle);
            for (int i = 1; i < colCount; i++) {
                Cell c = subRow.createCell(i);
                c.setCellStyle(subtitleStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, colCount - 1));

            // Encabezados
            Row headerRow = sheet.createRow(3);
            headerRow.setHeightInPoints(20);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Datos (con zebra striping)
            int rowIdx = 4;
            boolean alt = false;
            for (ReportDtos.DailySalesRow row : report.getRows()) {
                Row dataRow = sheet.createRow(rowIdx++);
                Cell c0 = dataRow.createCell(0);
                c0.setCellValue(row.getDay().toString());
                c0.setCellStyle(alt ? textStyleAlt : textStyle);

                Cell c1 = dataRow.createCell(1);
                c1.setCellValue(row.getCount());
                c1.setCellStyle(alt ? textStyleAlt : textStyle);

                Cell c2 = dataRow.createCell(2);
                c2.setCellValue(row.getRevenue().doubleValue());
                c2.setCellStyle(alt ? moneyStyleAlt : moneyStyle);

                Cell c3 = dataRow.createCell(3);
                c3.setCellValue(row.getNetRevenue().doubleValue());
                c3.setCellStyle(alt ? moneyStyleAlt : moneyStyle);

                alt = !alt;
            }

            // Fila de totales
            rowIdx++; // deja una fila en blanco antes del total
            Row totalRow = sheet.createRow(rowIdx);
            totalRow.setHeightInPoints(20);

            Cell tLabel = totalRow.createCell(0);
            tLabel.setCellValue("TOTAL");
            tLabel.setCellStyle(totalLabelStyle);

            Cell tCount = totalRow.createCell(1);
            tCount.setCellValue(report.getTotalSales());
            tCount.setCellStyle(totalLabelStyle);

            Cell tRevenue = totalRow.createCell(2);
            tRevenue.setCellValue(report.getTotalRevenue().doubleValue());
            tRevenue.setCellStyle(totalMoneyStyle);

            Cell tEmpty = totalRow.createCell(3);
            tEmpty.setCellStyle(totalLabelStyle);

            // Anchos de columna
            sheet.setColumnWidth(0, 16 * 256);
            sheet.setColumnWidth(1, 13 * 256);
            sheet.setColumnWidth(2, 18 * 256);
            sheet.setColumnWidth(3, 20 * 256);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ============================================================
    // EXPORTAR PDF: Ventas diarias
    // ============================================================
    public byte[] exportDailySalesPdf(LocalDate from, LocalDate to) throws Exception {
        ReportDtos.DailySalesReport report = getDailySalesReport(from, to);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);
        document.open();

        // Título
        com.itextpdf.text.Font titleFont =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.DARK_GRAY);
        document.add(new Paragraph("De los Reyes — Accesorios y Repuestos", titleFont));

        com.itextpdf.text.Font subFont =
            FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.GRAY);
        document.add(new Paragraph("Reporte de Ventas Diarias: " + from + " al " + to, subFont));
        document.add(new Paragraph("Total Ventas: " + report.getTotalSales()
            + " | Ingresos: S/ " + report.getTotalRevenue(), subFont));
        document.add(Chunk.NEWLINE);

        // Tabla
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 1.5f, 2f, 2f});

        String[] headers = {"Fecha", "N° Ventas", "Ingresos (S/)", "Neto (S/)"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE)));
            cell.setBackgroundColor(BaseColor.DARK_GRAY);
            cell.setPadding(5);
            table.addCell(cell);
        }

        for (ReportDtos.DailySalesRow row : report.getRows()) {
            table.addCell(row.getDay().toString());
            table.addCell(String.valueOf(row.getCount()));
            table.addCell("S/ " + row.getRevenue());
            table.addCell("S/ " + row.getNetRevenue());
        }

        document.add(table);
        document.close();

        return out.toByteArray();
    }

    // ============================================================
    // EXPORTAR EXCEL: Stock bajo
    // ============================================================
    public byte[] exportLowStockExcel() throws Exception {
        List<ReportDtos.LowStockRow> rows = getLowStockReport();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Stock Bajo");
            sheet.setDisplayGridlines(false);

            int colCount = 6;
            String[] headers = {"Código", "Producto", "Categoría", "Stock Actual", "Stock Mínimo", "Faltante"};

            CellStyle titleStyle   = buildTitleStyle(workbook);
            CellStyle headerStyle  = buildHeaderStyle(workbook);
            CellStyle textStyle    = buildDataStyle(workbook, false, false);
            CellStyle textStyleAlt = buildDataStyle(workbook, true, false);

            // Estilo especial para resaltar el faltante en rojo
            CellStyle shortageStyle = buildDataStyle(workbook, false, false);
            Font redFont = workbook.createFont();
            redFont.setBold(true);
            redFont.setColor(IndexedColors.RED.getIndex());
            shortageStyle.setFont(redFont);
            CellStyle shortageStyleAlt = buildDataStyle(workbook, true, false);
            shortageStyleAlt.setFont(redFont);

            // Título
            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(26);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Reporte de Stock Bajo — De los Reyes");
            titleCell.setCellStyle(titleStyle);
            for (int i = 1; i < colCount; i++) {
                Cell c = titleRow.createCell(i);
                c.setCellStyle(titleStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, colCount - 1));

            // Encabezados
            Row headerRow = sheet.createRow(2);
            headerRow.setHeightInPoints(20);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Datos
            int rowIdx = 3;
            boolean alt = false;
            for (ReportDtos.LowStockRow row : rows) {
                Row dataRow = sheet.createRow(rowIdx++);
                CellStyle style = alt ? textStyleAlt : textStyle;
                CellStyle shortStyle = alt ? shortageStyleAlt : shortageStyle;

                Cell c0 = dataRow.createCell(0);
                c0.setCellValue(row.getCode());
                c0.setCellStyle(style);

                Cell c1 = dataRow.createCell(1);
                c1.setCellValue(row.getName());
                c1.setCellStyle(style);

                Cell c2 = dataRow.createCell(2);
                c2.setCellValue(row.getCategory());
                c2.setCellStyle(style);

                Cell c3 = dataRow.createCell(3);
                c3.setCellValue(row.getCurrentStock());
                c3.setCellStyle(style);

                Cell c4 = dataRow.createCell(4);
                c4.setCellValue(row.getMinimumStock());
                c4.setCellStyle(style);

                Cell c5 = dataRow.createCell(5);
                c5.setCellValue(row.getShortage());
                c5.setCellStyle(shortStyle);

                alt = !alt;
            }

            sheet.setColumnWidth(0, 14 * 256);
            sheet.setColumnWidth(1, 30 * 256);
            sheet.setColumnWidth(2, 16 * 256);
            sheet.setColumnWidth(3, 14 * 256);
            sheet.setColumnWidth(4, 14 * 256);
            sheet.setColumnWidth(5, 12 * 256);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ============================================================
    // ESTILOS EXCEL — helpers reutilizables para todos los reportes
    // ============================================================

    /** Fondo azul oscuro de marca, texto blanco grande y en negrita — para el título fusionado de la hoja. */
    private CellStyle buildTitleStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(BRAND_DARK_BLUE, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }

    /** Texto gris pequeño para la fila de subtítulo (periodo del reporte). */
    private CellStyle buildSubtitleStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setItalic(true);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        return style;
    }

    /** Fondo azul de marca, texto blanco en negrita, bordes — para la fila de encabezados de columna. */
    private CellStyle buildHeaderStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(BRAND_DARK_BLUE, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
        applyThinBorder(style);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }

    /**
     * Estilo de celda de datos con bordes finos.
     * @param zebra   si true, aplica el fondo gris claro de fila alternada
     * @param money   si true, aplica formato de moneda "S/ #,##0.00"
     */
    private CellStyle buildDataStyle(XSSFWorkbook workbook, boolean zebra, boolean money) {
        XSSFCellStyle style = workbook.createCellStyle();
        applyThinBorder(style);
        if (zebra) {
            style.setFillForegroundColor(new XSSFColor(ZEBRA_GRAY, null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        if (money) {
            style.setDataFormat(workbook.createDataFormat().getFormat("\"S/\" #,##0.00"));
            style.setAlignment(HorizontalAlignment.RIGHT);
        }
        return style;
    }

    /**
     * Estilo para la fila de TOTAL: fondo dorado, negrita, borde superior grueso.
     * @param money si true, aplica formato de moneda además del resto.
     */
    private CellStyle buildTotalStyle(XSSFWorkbook workbook, boolean money) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(TOTAL_GOLD, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.MEDIUM);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        if (money) {
            style.setDataFormat(workbook.createDataFormat().getFormat("\"S/\" #,##0.00"));
            style.setAlignment(HorizontalAlignment.RIGHT);
        }
        return style;
    }

    private void applyThinBorder(XSSFCellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}