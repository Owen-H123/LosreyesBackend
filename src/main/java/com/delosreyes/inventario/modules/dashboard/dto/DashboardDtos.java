package com.delosreyes.inventario.modules.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DashboardDtos — Objetos de respuesta para el dashboard principal.
 *
 * QUÉ HACE:
 *   Agrupa todos los datos necesarios para la vista de inicio del ERP:
 *   - Totales del día / semana / mes
 *   - Alertas de stock bajo
 *   - Productos más vendidos
 *   - Movimientos recientes
 *   - Actividad por trabajador
 */
public final class DashboardDtos {

    private DashboardDtos() {}

    public static class Summary {
        private BigDecimal todayRevenue;
        private BigDecimal weekRevenue;
        private BigDecimal monthRevenue;
        private long todaySalesCount;
        private long weekSalesCount;
        private long monthSalesCount;
        private long lowStockCount;
        private long activeProductsCount;

        public BigDecimal getTodayRevenue() { return todayRevenue; }
        public void setTodayRevenue(BigDecimal todayRevenue) { this.todayRevenue = todayRevenue; }
        public BigDecimal getWeekRevenue() { return weekRevenue; }
        public void setWeekRevenue(BigDecimal weekRevenue) { this.weekRevenue = weekRevenue; }
        public BigDecimal getMonthRevenue() { return monthRevenue; }
        public void setMonthRevenue(BigDecimal monthRevenue) { this.monthRevenue = monthRevenue; }
        public long getTodaySalesCount() { return todaySalesCount; }
        public void setTodaySalesCount(long todaySalesCount) { this.todaySalesCount = todaySalesCount; }
        public long getWeekSalesCount() { return weekSalesCount; }
        public void setWeekSalesCount(long weekSalesCount) { this.weekSalesCount = weekSalesCount; }
        // Conteo de ventas del mes, para la tarjeta "Ventas este mes" del
        // dashboard (no confundir con weekSalesCount).
        public long getMonthSalesCount() { return monthSalesCount; }
        public void setMonthSalesCount(long monthSalesCount) { this.monthSalesCount = monthSalesCount; }
        public long getLowStockCount() { return lowStockCount; }
        public void setLowStockCount(long lowStockCount) { this.lowStockCount = lowStockCount; }
        public long getActiveProductsCount() { return activeProductsCount; }
        public void setActiveProductsCount(long activeProductsCount) { this.activeProductsCount = activeProductsCount; }
    }

    public static class TopProduct {
        private Long productId;
        private String code;
        private String name;
        private String imageUrl;
        private Long totalQuantity;
        private BigDecimal totalRevenue;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public Long getTotalQuantity() { return totalQuantity; }
        public void setTotalQuantity(Long totalQuantity) { this.totalQuantity = totalQuantity; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    }

    public static class WorkerActivity {
        private Long userId;
        private String userName;
        private String userAvatarUrl;
        private long salesCount;
        private BigDecimal totalRevenue;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getUserAvatarUrl() { return userAvatarUrl; }
        public void setUserAvatarUrl(String userAvatarUrl) { this.userAvatarUrl = userAvatarUrl; }
        public long getSalesCount() { return salesCount; }
        public void setSalesCount(long salesCount) { this.salesCount = salesCount; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    }

    public static class DashboardData {
        private Summary summary;
        private List<TopProduct> topProducts;
        private List<WorkerActivity> workerActivity;

        public Summary getSummary() { return summary; }
        public void setSummary(Summary summary) { this.summary = summary; }
        public List<TopProduct> getTopProducts() { return topProducts; }
        public void setTopProducts(List<TopProduct> topProducts) { this.topProducts = topProducts; }
        public List<WorkerActivity> getWorkerActivity() { return workerActivity; }
        public void setWorkerActivity(List<WorkerActivity> workerActivity) { this.workerActivity = workerActivity; }
    }

    public static class DailyRevenue {
        private String day;           // "Lun", "Mar", ...
        private java.time.LocalDate date;
        private BigDecimal revenue;
        private long count;

        public String getDay() { return day; }
        public void setDay(String day) { this.day = day; }
        public java.time.LocalDate getDate() { return date; }
        public void setDate(java.time.LocalDate date) { this.date = date; }
        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }
}