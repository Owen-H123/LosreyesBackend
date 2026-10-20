package com.delosreyes.inventario.modules.sales.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.delosreyes.inventario.modules.sales.entity.Sale;

import jakarta.persistence.LockModeType;

/**
 * SaleRepository — Acceso a datos de ventas con consultas para reportes y dashboard.
 *
 * QUÉ HACE:
 *   Persiste y consulta ventas. Provee queries analíticos para reportes
 *   de ganancias, ventas por día, productos más vendidos y totales.
 *
 * RELACIONES:
 *   Usado por SaleService (operaciones) y ReportService (analytics).
 */
@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    Optional<Sale> findBySaleNumber(String saleNumber);

    // Bloquea la venta hasta que termine la transacción (usado al cancelar)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Sale s WHERE s.id = :id")
    Optional<Sale> findByIdForUpdate(@Param("id") Long id);

    // Siguiente correlativo para el número de venta (secuencia creada en V5)
    @Query(value = "SELECT nextval('sale_number_seq')", nativeQuery = true)
    long nextSaleNumberSequence();

    Page<Sale> findByUserIdOrderBySaleDateDesc(Long userId, Pageable pageable);

    Page<Sale> findByCustomerIdOrderBySaleDateDesc(Long customerId, Pageable pageable);

    Page<Sale> findByStatusOrderBySaleDateDesc(Sale.Status status, Pageable pageable);

    @Query("""
        SELECT s FROM Sale s
        WHERE s.saleDate BETWEEN :from AND :to
        ORDER BY s.saleDate DESC
        """)
    Page<Sale> findByDateRange(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        Pageable pageable);

    // ── Dashboard ──────────────────────────────────────────────

    @Query("""
        SELECT COALESCE(SUM(s.total), 0)
        FROM Sale s
        WHERE s.status = 'COMPLETED'
          AND s.saleDate BETWEEN :from AND :to
        """)
    BigDecimal sumTotalByDateRange(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to);

    @Query("""
        SELECT COUNT(s)
        FROM Sale s
        WHERE s.status = 'COMPLETED'
          AND s.saleDate BETWEEN :from AND :to
        """)
    long countByDateRange(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to);

    // Ventas del día agrupadas por hora (para gráfico)
    @Query(value = """
        SELECT DATE_TRUNC('hour', sale_date) AS hour,
               COUNT(*) AS count,
               SUM(total) AS total
        FROM sales
        WHERE status = 'COMPLETED'
          AND sale_date >= :since
        GROUP BY DATE_TRUNC('hour', sale_date)
        ORDER BY hour
        """, nativeQuery = true)
    List<Object[]> findSalesByHour(@Param("since") LocalDateTime since);

    // Ganancias diarias en un rango
    @Query(value = """
        SELECT DATE(sale_date) AS day,
               COUNT(*) AS count,
               SUM(total) AS revenue,
               SUM(total - discount) AS net_revenue
        FROM sales
        WHERE status = 'COMPLETED'
          AND sale_date BETWEEN :from AND :to
        GROUP BY DATE(sale_date)
        ORDER BY day
        """, nativeQuery = true)
    List<Object[]> findDailyRevenue(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to);

    // Ventas recientes (últimas N)
    @Query("SELECT s FROM Sale s ORDER BY s.saleDate DESC")
    List<Sale> findRecentSales(Pageable pageable);

    // Totales por método de pago en rango de fechas
    @Query(value = """
        SELECT payment_method,
               COUNT(*) AS count,
               SUM(total) AS total
        FROM sales
        WHERE status = 'COMPLETED'
          AND sale_date BETWEEN :from AND :to
        GROUP BY payment_method
        """, nativeQuery = true)
    List<Object[]> findRevenueByPaymentMethod(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to);

    @Query("""
        SELECT COALESCE(SUM(s.total), 0)
        FROM Sale s
        WHERE s.status = 'COMPLETED'
          AND s.user.id = :userId
          AND s.saleDate BETWEEN :from AND :to
        """)
    BigDecimal sumTotalByUserAndDateRange(
        @Param("userId") Long userId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to);

    // Ventas completadas de un trabajador en un rango de fechas. Lo usa
    // getWorkerActivity() (DashboardService) para la sección "Equipo hoy".
    @Query("""
        SELECT COUNT(s)
        FROM Sale s
        WHERE s.status = 'COMPLETED'
          AND s.user.id = :userId
          AND s.saleDate BETWEEN :from AND :to
        """)
    long countByUserAndDateRange(
        @Param("userId") Long userId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to);
}