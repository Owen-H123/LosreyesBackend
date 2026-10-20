package com.delosreyes.inventario.modules.dashboard.service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delosreyes.inventario.modules.dashboard.dto.DashboardDtos;
import com.delosreyes.inventario.modules.movements.dto.MovementDtos;
import com.delosreyes.inventario.modules.movements.service.InventoryMovementService;
import com.delosreyes.inventario.modules.products.dto.ProductDtos;
import com.delosreyes.inventario.modules.products.service.ProductService;
import com.delosreyes.inventario.modules.sales.dto.SaleDtos;
import com.delosreyes.inventario.modules.sales.repository.SaleItemRepository;
import com.delosreyes.inventario.modules.sales.repository.SaleRepository;
import com.delosreyes.inventario.modules.sales.service.SaleService;
import com.delosreyes.inventario.modules.users.repository.UserRepository;
/**
 * DashboardService — Agregador de datos para el panel de control.
 *
 * QUÉ HACE:
 *   Recopila y agrega datos de múltiples módulos para el dashboard:
 *   - Totales de ventas (día, semana, mes)
 *   - Conteo de productos con stock bajo
 *   - Top 5 productos más vendidos del mes
 *   - Actividad de trabajadores
 *   - Movimientos recientes
 *   - Ventas recientes
 *
 * CACHÉ:
 *   El resumen principal se cachea 5 minutos (TTL configurable).
 *   Evita N consultas a BD en cada recarga del dashboard.
 *
 * RELACIONES:
 *   Orquesta datos de SaleRepository, ProductRepository, UserRepository,
 *   SaleItemRepository y InventoryMovementService.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final SaleService saleService;
    private final ProductService productService;
    private final InventoryMovementService movementService;
    private final UserRepository userRepository;

    public DashboardService(SaleRepository saleRepository,
                             SaleItemRepository saleItemRepository,
                             SaleService saleService,
                             ProductService productService,
                             InventoryMovementService movementService,
                             UserRepository userRepository) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.saleService = saleService;
        this.productService = productService;
        this.movementService = movementService;
        this.userRepository = userRepository;
    }

    // ============================================================
    // RESUMEN PRINCIPAL
    // ============================================================
    @Cacheable(value = "dashboard", key = "'summary'")
    public DashboardDtos.Summary getSummary() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd   = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime weekStart  = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        DashboardDtos.Summary summary = new DashboardDtos.Summary();

        // Ingresos
        summary.setTodayRevenue(orZero(
            saleRepository.sumTotalByDateRange(todayStart, todayEnd)));
        summary.setWeekRevenue(orZero(
            saleRepository.sumTotalByDateRange(weekStart, todayEnd)));
        summary.setMonthRevenue(orZero(
            saleRepository.sumTotalByDateRange(monthStart, todayEnd)));

        // Conteos de ventas
        summary.setTodaySalesCount(saleRepository.countByDateRange(todayStart, todayEnd));
        summary.setWeekSalesCount(saleRepository.countByDateRange(weekStart, todayEnd));
        summary.setMonthSalesCount(saleRepository.countByDateRange(monthStart, todayEnd));

        // Inventario
        summary.setLowStockCount(productService.countLowStock());
        summary.setActiveProductsCount(productService.countActive());

        return summary;
    }

    // ============================================================
    // DATOS COMPLETOS DEL DASHBOARD
    // ============================================================
    public DashboardDtos.DashboardData getDashboardData() {
        DashboardDtos.DashboardData data = new DashboardDtos.DashboardData();

        data.setSummary(getSummary());
        data.setTopProducts(getTopProducts(5));
        data.setWorkerActivity(getWorkerActivity());

        return data;
    }

    // ============================================================
    // TOP PRODUCTOS DEL MES
    // ============================================================
    public List<DashboardDtos.TopProduct> getTopProducts(int limit) {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        List<Object[]> rows = saleItemRepository.findTopProducts(monthStart, now, limit);

        return rows.stream().map(row -> {
            DashboardDtos.TopProduct tp = new DashboardDtos.TopProduct();
            tp.setProductId(((Number) row[0]).longValue());
            tp.setCode((String) row[1]);
            tp.setName((String) row[2]);
            tp.setTotalQuantity(((Number) row[3]).longValue());
            tp.setTotalRevenue(new BigDecimal(row[4].toString()));
            tp.setImageUrl(row[5] != null ? (String) row[5] : null);
            return tp;
        }).collect(Collectors.toList());
    }

    // ============================================================
    // ACTIVIDAD DE TRABAJADORES HOY
    // ============================================================
    public List<DashboardDtos.WorkerActivity> getWorkerActivity() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd   = LocalDateTime.now();

        return userRepository.findAll().stream()
            .map(user -> {
                BigDecimal revenue = saleRepository.sumTotalByUserAndDateRange(
                    user.getId(), todayStart, todayEnd);
                long count = saleRepository.countByUserAndDateRange(
                    user.getId(), todayStart, todayEnd);

                DashboardDtos.WorkerActivity wa = new DashboardDtos.WorkerActivity();
                wa.setUserId(user.getId());
                wa.setUserName(user.getName());
                wa.setUserAvatarUrl(user.getAvatarUrl());
                wa.setSalesCount(count);
                wa.setTotalRevenue(orZero(revenue));
                return wa;
            })
            .filter(wa -> wa.getSalesCount() > 0)
            .collect(Collectors.toList());
    }

    // ============================================================
    // VENTAS RECIENTES (para tabla en dashboard)
    // ============================================================
    public List<SaleDtos.SaleResponse> getRecentSales(int limit) {
        return saleService.findRecent(limit);
    }

    // ============================================================
    // MOVIMIENTOS RECIENTES (para tabla en dashboard)
    // ============================================================
    public List<MovementDtos.MovementResponse> getRecentMovements(int limit) {
        return movementService.findRecent(limit);
    }

    // ============================================================
    // INGRESOS POR DÍA — para el gráfico "Últimos N días" del dashboard
    //
    // Antes, el gráfico consumía /dashboard/recent-sales (una lista de
    // VENTAS individuales, no agregadas por día), así que no encontraba
    // los campos "day"/"revenue" que espera el AreaChart y se veía vacío.
    // Este método sí agrupa por día, incluyendo días sin ventas (revenue=0),
    // para que la tendencia se vea completa aunque no todos los días tengan
    // transacciones.
    // ============================================================
    public List<DashboardDtos.DailyRevenue> getDailyRevenue(int days) {
        List<DashboardDtos.DailyRevenue> result = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd   = date.atTime(LocalTime.MAX);

            DashboardDtos.DailyRevenue dr = new DashboardDtos.DailyRevenue();
            dr.setDate(date);
            dr.setDay(capitalize(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, new Locale("es", "ES"))));
            dr.setRevenue(orZero(saleRepository.sumTotalByDateRange(dayStart, dayEnd)));
            dr.setCount(saleRepository.countByDateRange(dayStart, dayEnd));

            result.add(dr);
        }

        return result;
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) return value;
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
    // ============================================================
    // PRODUCTOS CON STOCK BAJO
    // ============================================================
    public List<ProductDtos.ProductResponse> getLowStockProducts() {
        return productService.findLowStock();
    }

    // ============================================================
    // Helpers
    // ============================================================
    private BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}