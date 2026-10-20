package com.delosreyes.inventario.modules.reports.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.delosreyes.inventario.modules.reports.dto.ReportDtos;
import com.delosreyes.inventario.modules.reports.service.ReportService;
import com.delosreyes.inventario.shared.responses.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * ReportController — Endpoints para generación y descarga de reportes.
 *
 * ACCESO: Solo ADMIN (@PreAuthorize)
 *
 * ENDPOINTS JSON:
 *   GET /reports/sales/daily          — Ventas diarias (JSON)
 *   GET /reports/sales/top-products   — Productos más vendidos (JSON)
 *   GET /reports/inventory/low-stock  — Stock bajo (JSON)
 *
 * ENDPOINTS EXCEL:
 *   GET /reports/sales/daily/excel    — Descarga .xlsx
 *   GET /reports/inventory/low-stock/excel
 *
 * ENDPOINTS PDF:
 *   GET /reports/sales/daily/pdf      — Descarga .pdf
 *
 * PARÁMETROS ESTÁNDAR:
 *   from: fecha inicio (ISO: yyyy-MM-dd)
 *   to:   fecha fin    (ISO: yyyy-MM-dd)
 */
@RestController
@RequestMapping("/reports")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','PERM_VIEW_REPORTS')")
@Tag(name = "Reportes", description = "Reportes analíticos del negocio (solo ADMIN)")
@SecurityRequirement(name = "Bearer Authentication")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // ============================================================
    // VENTAS DIARIAS
    // ============================================================

    @GetMapping("/sales/daily")
    @Operation(summary = "Reporte de ventas diarias en rango de fechas (JSON)")
    public ResponseEntity<ApiResponse<ReportDtos.DailySalesReport>> getDailySales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(ApiResponse.success("Reporte generado",
            reportService.getDailySalesReport(from, to)));
    }

    @GetMapping("/sales/daily/excel")
    @Operation(summary = "Descargar reporte de ventas diarias como Excel (.xlsx)")
    public ResponseEntity<byte[]> getDailySalesExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) throws Exception {

        byte[] data = reportService.exportDailySalesExcel(from, to);
        String filename = "ventas-diarias-" + from + "-a-" + to + ".xlsx";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(data);
    }

    @GetMapping("/sales/daily/pdf")
    @Operation(summary = "Descargar reporte de ventas diarias como PDF")
    public ResponseEntity<byte[]> getDailySalesPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) throws Exception {

        byte[] data = reportService.exportDailySalesPdf(from, to);
        String filename = "ventas-diarias-" + from + "-a-" + to + ".pdf";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(data);
    }

    // ============================================================
    // TOP PRODUCTOS
    // ============================================================

    @GetMapping("/sales/top-products")
    @Operation(summary = "Productos más vendidos en un período (JSON)")
    public ResponseEntity<ApiResponse<List<ReportDtos.TopProductRow>>> getTopProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(ApiResponse.success("Top productos",
            reportService.getTopProductsReport(from, to, limit)));
    }

    // ============================================================
    // STOCK BAJO
    // ============================================================

    @GetMapping("/inventory/low-stock")
    @Operation(summary = "Reporte de productos con stock bajo (JSON)")
    public ResponseEntity<ApiResponse<List<ReportDtos.LowStockRow>>> getLowStock() {
        return ResponseEntity.ok(ApiResponse.success("Reporte stock bajo",
            reportService.getLowStockReport()));
    }

    @GetMapping("/inventory/low-stock/excel")
    @Operation(summary = "Descargar reporte de stock bajo como Excel (.xlsx)")
    public ResponseEntity<byte[]> getLowStockExcel() throws Exception {
        byte[] data = reportService.exportLowStockExcel();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"stock-bajo-" + LocalDate.now() + ".xlsx\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(data);
    }
}