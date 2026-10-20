package com.delosreyes.inventario.modules.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * ReportDtos — DTOs para generación de reportes.
 *
 * QUÉ HACE:
 *   Define las estructuras de salida de todos los reportes del sistema.
 *   Cada reporte puede exportarse como JSON (API), PDF o Excel.
 *
 * REPORTES DISPONIBLES:
 *   - Ventas diarias: ingresos agrupados por día en un rango
 *   - Ganancias: diferencia entre precio venta y costo en un período
 *   - Productos más vendidos: top N por cantidad y revenue
 *   - Stock bajo: productos que requieren reposición
 *   - Movimientos: historial de entradas/salidas en un período
 */
public final class ReportDtos {

    private ReportDtos() {}

    // ── Ventas diarias ────────────────────────────────────────
    public static class DailySalesRow {
        private LocalDate day;
        private long count;
        private BigDecimal revenue;
        private BigDecimal netRevenue;

        public LocalDate getDay() { return day; }
        public void setDay(LocalDate day) { this.day = day; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
        public BigDecimal getNetRevenue() { return netRevenue; }
        public void setNetRevenue(BigDecimal netRevenue) { this.netRevenue = netRevenue; }
    }

    public static class DailySalesReport {
        private LocalDate from;
        private LocalDate to;
        private BigDecimal totalRevenue;
        private long totalSales;
        private List<DailySalesRow> rows;

        public LocalDate getFrom() { return from; }
        public void setFrom(LocalDate from) { this.from = from; }
        public LocalDate getTo() { return to; }
        public void setTo(LocalDate to) { this.to = to; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
        public long getTotalSales() { return totalSales; }
        public void setTotalSales(long totalSales) { this.totalSales = totalSales; }
        public List<DailySalesRow> getRows() { return rows; }
        public void setRows(List<DailySalesRow> rows) { this.rows = rows; }
    }

    // ── Productos más vendidos ─────────────────────────────────
    public static class TopProductRow {
        private Long productId;
        private String code;
        private String name;
        private long totalQty;
        private BigDecimal totalRevenue;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public long getTotalQty() { return totalQty; }
        public void setTotalQty(long totalQty) { this.totalQty = totalQty; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    }

    // ── Stock bajo ─────────────────────────────────────────────
    public static class LowStockRow {
        private Long productId;
        private String code;
        private String name;
        private String category;
        private int currentStock;
        private int minimumStock;
        private int shortage;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public int getCurrentStock() { return currentStock; }
        public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }
        public int getMinimumStock() { return minimumStock; }
        public void setMinimumStock(int minimumStock) { this.minimumStock = minimumStock; }
        public int getShortage() { return shortage; }
        public void setShortage(int shortage) { this.shortage = shortage; }
    }
}