package com.delosreyes.inventario.modules.products.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.delosreyes.inventario.modules.products.entity.Product;

import jakarta.persistence.LockModeType;

/**
 * ProductRepository — Repositorio de productos con búsquedas avanzadas.
 *
 * QUÉ HACE:
 *   Provee consultas paginadas, filtradas y búsquedas full-text para productos.
 *   Implementa JpaSpecificationExecutor para filtros dinámicos (Specification pattern).
 *
 * TODOS LOS QUERIES filtran deleted = FALSE para respetar el soft delete.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByCodeAndDeletedFalse(String code);

    // Carga el producto con SELECT ... FOR UPDATE: bloquea la fila hasta que
    // termina la transacción. Si dos operaciones cambian el stock del mismo
    // producto a la vez, la segunda espera y lee el stock ya actualizado.
    // Sin esto, las dos leerían el mismo valor y se perdería una de las restas.
    // Lo usan ventas, cancelaciones y movimientos manuales, a través de
    // ProductService.findEntityByIdForUpdate.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deleted = FALSE")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    boolean existsByCodeAndDeletedFalse(String code);

    boolean existsByCodeAndDeletedFalseAndIdNot(String code, Long id);

    // Búsqueda full-text por código, nombre o marca
    @Query("""
        SELECT p FROM Product p
        WHERE p.deleted = FALSE
          AND (
            LOWER(p.code) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        """)
    Page<Product> search(@Param("query") String query, Pageable pageable);

    // Productos con stock bajo (stock <= minimumStock)
    @Query("SELECT p FROM Product p WHERE p.deleted = FALSE AND p.stock <= p.minimumStock AND p.status = 'ACTIVE'")
    List<Product> findLowStock();

    @Query("SELECT p FROM Product p WHERE p.deleted = FALSE AND p.stock <= p.minimumStock AND p.status = 'ACTIVE'")
    Page<Product> findLowStockPaged(Pageable pageable);

    // Por categoría
    Page<Product> findByCategoryIdAndDeletedFalse(Long categoryId, Pageable pageable);

    // Por estado
    Page<Product> findByStatusAndDeletedFalse(Product.Status status, Pageable pageable);

    // Por vehículo compatible
    @Query("""
        SELECT p FROM Product p
        JOIN p.vehicles v
        WHERE p.deleted = FALSE
          AND v.id = :vehicleId
        """)
    Page<Product> findByVehicleId(@Param("vehicleId") Long vehicleId, Pageable pageable);

    // Para reportes: productos más vendidos
    @Query("""
        SELECT p.id, p.code, p.name, SUM(si.quantity) AS totalSold
        FROM Product p
        JOIN SaleItem si ON si.product.id = p.id
        JOIN si.sale s
        WHERE s.status = 'COMPLETED'
          AND s.saleDate >= :fromDate
        GROUP BY p.id, p.code, p.name
        ORDER BY totalSold DESC
        """)
    List<Object[]> findTopSellingProducts(
        @Param("fromDate") java.time.LocalDateTime fromDate,
        Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.deleted = FALSE AND p.stock <= p.minimumStock AND p.status = 'ACTIVE'")
    long countLowStock();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.deleted = FALSE AND p.status = 'ACTIVE'")
    long countActive();
}