package com.delosreyes.inventario.modules.weborders.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.delosreyes.inventario.modules.weborders.entity.WebOrder;

public interface WebOrderRepository extends JpaRepository<WebOrder, Long>, JpaSpecificationExecutor<WebOrder> {

    Page<WebOrder> findByStatus(WebOrder.Status status, Pageable pageable);

    long countByStatus(WebOrder.Status status);

    long countByPaymentStatus(WebOrder.PaymentStatus paymentStatus);

    java.util.Optional<WebOrder> findByOrderNumber(String orderNumber);

    boolean existsByPaymentOperationCodeAndPaymentStatusIn(String code, java.util.Collection<WebOrder.PaymentStatus> statuses);

    long countByCreatedAtGreaterThanEqual(LocalDateTime from);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM WebOrder o WHERE o.status <> 'CANCELLED' AND o.createdAt >= :from")
    BigDecimal sumTotalSince(@Param("from") LocalDateTime from);

    @Query(value = "SELECT nextval('web_order_number_seq')", nativeQuery = true)
    long nextNumber();

    /** Clientes de la web agrupados por celular: [celular, nombre más reciente, pedidos, gastado, último pedido]. */
    @Query("""
        SELECT o.phone, MAX(o.customerName), COUNT(o),
               COALESCE(SUM(CASE WHEN o.status <> 'CANCELLED' THEN o.total ELSE 0 END), 0),
               MAX(o.createdAt)
        FROM WebOrder o GROUP BY o.phone ORDER BY MAX(o.createdAt) DESC
        """)
    List<Object[]> customerStats();

    /** Productos más pedidos desde la web (sin cancelados): [código, nombre, unidades, importe]. */
    @Query("""
        SELECT i.productCode, MAX(i.productName), SUM(i.quantity), SUM(i.quantity * i.unitPrice)
        FROM WebOrderItem i WHERE i.order.status <> 'CANCELLED' AND i.order.createdAt >= :from
        GROUP BY i.productCode ORDER BY SUM(i.quantity) DESC
        """)
    List<Object[]> topProductsSince(@Param("from") LocalDateTime from, Pageable pageable);
}
