package com.delosreyes.inventario.modules.dashboard.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.delosreyes.inventario.modules.dashboard.dto.DashboardDtos;
import com.delosreyes.inventario.modules.dashboard.service.DashboardService;
import com.delosreyes.inventario.modules.movements.dto.MovementDtos;
import com.delosreyes.inventario.modules.products.dto.ProductDtos;
import com.delosreyes.inventario.modules.sales.dto.SaleDtos;
import com.delosreyes.inventario.shared.responses.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * DashboardController — Endpoints del panel de control.
 *
 * ACCESO: ADMIN + WORKER
 *
 * ENDPOINTS:
 *   GET /dashboard              — Datos completos del dashboard
 *   GET /dashboard/summary      — Resumen de ventas e inventario (cacheado)
 *   GET /dashboard/top-products — Productos más vendidos
 *   GET /dashboard/low-stock    — Productos con stock bajo
 *   GET /dashboard/recent-sales — Últimas ventas
 *   GET /dashboard/recent-movements — Últimos movimientos
 *   GET /dashboard/worker-activity  — Actividad de trabajadores
 */
@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "Panel de control y métricas del negocio")
@SecurityRequirement(name = "Bearer Authentication")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Obtener todos los datos del dashboard en una sola llamada")
    public ResponseEntity<ApiResponse<DashboardDtos.DashboardData>> getDashboard() {
        return ResponseEntity.ok(
            ApiResponse.success("Dashboard cargado", dashboardService.getDashboardData()));
    }

    @GetMapping("/summary")
    @Operation(summary = "Resumen de ventas e inventario (cacheado, ideal para polling)")
    public ResponseEntity<ApiResponse<DashboardDtos.Summary>> getSummary() {
        return ResponseEntity.ok(
            ApiResponse.success("Resumen obtenido", dashboardService.getSummary()));
    }

    @GetMapping("/top-products")
    @Operation(summary = "Productos más vendidos del mes")
    public ResponseEntity<ApiResponse<List<DashboardDtos.TopProduct>>> getTopProducts(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(
            ApiResponse.success("Top productos", dashboardService.getTopProducts(limit)));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Productos con stock bajo o agotado")
    public ResponseEntity<ApiResponse<List<ProductDtos.ProductResponse>>> getLowStock() {
        return ResponseEntity.ok(
            ApiResponse.success("Productos con stock bajo", dashboardService.getLowStockProducts()));
    }

    @GetMapping("/recent-sales")
    @Operation(summary = "Ventas más recientes")
    public ResponseEntity<ApiResponse<List<SaleDtos.SaleResponse>>> getRecentSales(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(
            ApiResponse.success("Ventas recientes", dashboardService.getRecentSales(limit)));
    }
@GetMapping("/daily-revenue")
    @Operation(summary = "Ingresos agrupados por día, últimos N días (para el gráfico de tendencia)")
    public ResponseEntity<ApiResponse<List<DashboardDtos.DailyRevenue>>> getDailyRevenue(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(
            ApiResponse.success("Ingresos diarios", dashboardService.getDailyRevenue(days)));
    }
    @GetMapping("/recent-movements")
    @Operation(summary = "Últimos movimientos de inventario")
    public ResponseEntity<ApiResponse<List<MovementDtos.MovementResponse>>> getRecentMovements(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(
            ApiResponse.success("Movimientos recientes", dashboardService.getRecentMovements(limit)));
    }

    @GetMapping("/worker-activity")
    @Operation(summary = "Actividad de trabajadores hoy")
    public ResponseEntity<ApiResponse<List<DashboardDtos.WorkerActivity>>> getWorkerActivity() {
        return ResponseEntity.ok(
            ApiResponse.success("Actividad de trabajadores", dashboardService.getWorkerActivity()));
    }
}