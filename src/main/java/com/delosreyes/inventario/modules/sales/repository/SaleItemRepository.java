package com.delosreyes.inventario.modules.sales.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.delosreyes.inventario.modules.sales.entity.SaleItem;

/**
 * SaleItemRepository — Acceso a items de venta.
 *
 * QUÉ HACE:
 *   Provee consultas sobre los detalles de venta.
 *   Se usa principalmente en reportes de productos más vendidos.
 */
@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    List<SaleItem> findBySaleId(Long saleId);

    // Productos más vendidos en rango de fechas
    @Query(value = """
        SELECT p.id,
               p.code,
               p.name,
               SUM(si.quantity)   AS total_qty,
               SUM(si.subtotal)   AS total_revenue,
               p.image_url        AS image_url
        FROM sale_items si
        JOIN products p ON p.id = si.product_id
        JOIN sales s     ON s.id = si.sale_id
        WHERE s.status = 'COMPLETED'
          AND s.sale_date BETWEEN :from AND :to
        GROUP BY p.id, p.code, p.name, p.image_url
        ORDER BY total_qty DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findTopProducts(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        @Param("limit") int limit);
}