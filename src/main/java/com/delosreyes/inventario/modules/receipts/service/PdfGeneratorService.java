package com.delosreyes.inventario.modules.receipts.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.delosreyes.inventario.modules.receipts.dto.ReceiptDtos;
import com.delosreyes.inventario.shared.exceptions.BusinessException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * PdfGeneratorService — Construye el documento PDF de la boleta usando iText 5
 * (dependencia ya existente en el proyecto: com.itextpdf:itextpdf).
 *
 * QUÉ HACE:
 *   Genera un PDF con el membrete de la empresa (incluye logo), datos de
 *   la venta, datos del cliente, tabla de productos y totales.
 *
 * LOGO:
 *   Se carga como recurso embebido del classpath (src/main/resources/static/logo.png),
 *   NO desde el sistema de archivos ni desde /uploads. Esto es intencional: es un
 *   activo fijo de la empresa que debe viajar empaquetado dentro del .jar, no
 *   depender de una ruta externa que podría no existir en otro entorno/PC.
 *   Si el archivo no está presente, el PDF se genera igual (sin logo) para que
 *   un recurso faltante nunca tumbe la generación de boletas.
 *
 * NO DEPENDE DE:
 *   Thymeleaf ni starter de mail (no están en el pom.xml). El HTML de
 *   receipt.html se usa como plantilla de referencia / para el futuro
 *   envío por correo (EmailService), no para generar este PDF.
 */
@Service
public class PdfGeneratorService {

    private static final String COMPANY_NAME = "De Los Reyes S.A.C";
    private static final String COMPANY_SLOGAN = "Repuestos y accesorios automotrices";

    // Ruta dentro de src/main/resources — se resuelve vía classloader, no filesystem.
    private static final String LOGO_CLASSPATH = "/static/logo.png";
    private static final float LOGO_MAX_WIDTH = 70f;
    private static final float LOGO_MAX_HEIGHT = 70f;

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.BLACK);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.DARK_GRAY);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.BLACK);
    private static final Font FONT_LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.BLACK);
    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.BLACK);
    private static final Font FONT_HEADER_TABLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE);
    private static final Font FONT_TOTAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLACK);

    private static final BaseColor BRAND_COLOR = new BaseColor(30, 41, 59); // slate oscuro

    public byte[] generateReceiptPdf(ReceiptDtos.ReceiptData data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document);
            addSaleInfo(document, data);
            addCustomerInfo(document, data);
            addItemsTable(document, data);
            addTotals(document, data);
            addFooter(document);

            document.close();
            return out.toByteArray();

        } catch (DocumentException | java.io.IOException ex) {
            throw new BusinessException("PDF_GENERATION_ERROR",
                "No se pudo generar el PDF de la boleta: " + ex.getMessage());
        }
    }

    // ============================================================
    // Secciones del documento
    // ============================================================

    private void addHeader(Document document) throws DocumentException {
        Image logo = loadLogo();

        if (logo != null) {
            // Tabla de 2 columnas: logo a la izquierda, nombre/slogan a la derecha
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{0.6f, 4f});

            PdfPCell logoCell = new PdfPCell(logo, false);
            logoCell.setBorder(PdfPCell.NO_BORDER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            logoCell.setPadding(0f);
            logoCell.setPaddingRight(8f);
            headerTable.addCell(logoCell);

            PdfPCell textCell = new PdfPCell();
            textCell.setBorder(PdfPCell.NO_BORDER);
            textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            textCell.setPadding(0f);

            Paragraph company = new Paragraph(COMPANY_NAME, FONT_TITLE);
            Paragraph slogan = new Paragraph(COMPANY_SLOGAN, FONT_SUBTITLE);
            textCell.addElement(company);
            textCell.addElement(slogan);
            headerTable.addCell(textCell);

            document.add(headerTable);
        } else {
            // Sin logo disponible: mismo layout de siempre, sin romper nada
            Paragraph company = new Paragraph(COMPANY_NAME, FONT_TITLE);
            company.setAlignment(Element.ALIGN_LEFT);
            document.add(company);

            Paragraph slogan = new Paragraph(COMPANY_SLOGAN, FONT_SUBTITLE);
            document.add(slogan);
        }

        Paragraph spacerAfterHeader = spacer();
        spacerAfterHeader.setSpacingAfter(2f);
        document.add(spacerAfterHeader);

        // Línea separadora
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setBorderWidthTop(1.5f);
        cell.setBorderColorTop(BRAND_COLOR);
        cell.setBorderWidthBottom(0);
        cell.setBorderWidthLeft(0);
        cell.setBorderWidthRight(0);
        cell.setFixedHeight(1f);
        line.addCell(cell);
        document.add(line);

        document.add(spacer());
    }

    /**
     * Carga el logo desde el classpath (src/main/resources/static/logo.png).
     * Devuelve null si el recurso no existe o no se puede leer — así la boleta
     * se sigue generando normalmente (sin logo) en vez de fallar por completo.
     */
    private Image loadLogo() {
        try (InputStream is = getClass().getResourceAsStream(LOGO_CLASSPATH)) {
            if (is == null) {
                return null;
            }
            byte[] bytes = is.readAllBytes();
            Image logo = Image.getInstance(bytes);
            logo.scaleToFit(LOGO_MAX_WIDTH, LOGO_MAX_HEIGHT);
            return logo;
        } catch (Exception ex) {
            // No tumbar la generación del PDF por un logo faltante o corrupto
            return null;
        }
    }

    private void addSaleInfo(Document document, ReceiptDtos.ReceiptData data) throws DocumentException {
        Paragraph title = new Paragraph("BOLETA DE VENTA", FONT_SECTION);
        title.setSpacingBefore(8f);
        title.setSpacingAfter(6f);
        document.add(title);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 1f});

        addInfoRow(table, "N° de venta:", data.getSaleNumber());
        addInfoRow(table, "Fecha:", data.getSaleDate() != null ? data.getSaleDate().format(DATE_FORMATTER) : "-");
        addInfoRow(table, "Atendido por:", data.getSellerName());
        addInfoRow(table, "Método de pago:", translatePaymentMethod(data.getPaymentMethod()));

        document.add(table);
    }

    private void addCustomerInfo(Document document, ReceiptDtos.ReceiptData data) throws DocumentException {
        Paragraph title = new Paragraph("DATOS DEL CLIENTE", FONT_SECTION);
        title.setSpacingBefore(10f);
        title.setSpacingAfter(6f);
        document.add(title);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 1f});

        addInfoRow(table, "Cliente:", data.getCustomerName());
        addInfoRow(table, "Documento:",
            data.getCustomerDocumentType() + " " + data.getCustomerDocumentNumber());

        document.add(table);
    }

    private void addItemsTable(Document document, ReceiptDtos.ReceiptData data) throws DocumentException {
        Paragraph title = new Paragraph("PRODUCTOS", FONT_SECTION);
        title.setSpacingBefore(10f);
        title.setSpacingAfter(6f);
        document.add(title);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.2f, 4f, 1f, 1.4f, 1.4f});

        addHeaderCell(table, "Código");
        addHeaderCell(table, "Producto");
        addHeaderCell(table, "Cant.");
        addHeaderCell(table, "P. Unit.");
        addHeaderCell(table, "Subtotal");

        for (ReceiptDtos.ReceiptItem item : data.getItems()) {
            addBodyCell(table, item.getProductCode(), Element.ALIGN_LEFT);
            addBodyCell(table, item.getProductName(), Element.ALIGN_LEFT);
            addBodyCell(table, String.valueOf(item.getQuantity()), Element.ALIGN_CENTER);
            addBodyCell(table, "S/ " + item.getUnitPrice().toPlainString(), Element.ALIGN_RIGHT);
            addBodyCell(table, "S/ " + item.getSubtotal().toPlainString(), Element.ALIGN_RIGHT);
        }

        document.add(table);
    }

    private void addTotals(Document document, ReceiptDtos.ReceiptData data) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(50);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setWidths(new float[]{1f, 1f});
        table.setSpacingBefore(10f);

        addTotalRow(table, "Subtotal:", data.getSubtotal(), FONT_NORMAL);
        addTotalRow(table, "Descuento:", data.getDiscount(), FONT_NORMAL);
        // El IGV ya está dentro del total: se muestra como referencia, no se suma
        addTotalRow(table, "IGV (18%) incluido:", data.getTax(), FONT_NORMAL);

        PdfPCell labelCell = new PdfPCell(new Phrase("TOTAL:", FONT_TOTAL));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setBorderWidthTop(1f);
        labelCell.setBorderColorTop(BaseColor.BLACK);
        labelCell.setPaddingTop(6f);
        labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase("S/ " + data.getTotal().toPlainString(), FONT_TOTAL));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setBorderWidthTop(1f);
        valueCell.setBorderColorTop(BaseColor.BLACK);
        valueCell.setPaddingTop(6f);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);

        document.add(table);
    }

    private void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph(
            "Gracias por su compra en " + COMPANY_NAME, FONT_SUBTITLE);
        footer.setSpacingBefore(20f);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    // ============================================================
    // Helpers de construcción de tabla
    // ============================================================

    private void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FONT_LABEL));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setPaddingBottom(3f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "-", FONT_NORMAL));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setPaddingBottom(3f);
        table.addCell(valueCell);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_HEADER_TABLE));
        cell.setBackgroundColor(BRAND_COLOR);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", FONT_NORMAL));
        cell.setPadding(5f);
        cell.setHorizontalAlignment(alignment);
        cell.setBorderColor(BaseColor.LIGHT_GRAY);
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, java.math.BigDecimal value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase("S/ " + value.toPlainString(), font));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private Paragraph spacer() {
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingAfter(2f);
        return spacer;
    }

    private String translatePaymentMethod(String method) {
        if (method == null) return "-";
        return switch (method) {
            case "CASH" -> "Efectivo";
            case "CARD" -> "Tarjeta";
            case "TRANSFER" -> "Transferencia";
            case "CREDIT" -> "Crédito";
            default -> method;
        };
    }
}