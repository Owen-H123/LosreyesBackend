package com.delosreyes.inventario.modules.movements.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.delosreyes.inventario.modules.movements.entity.InventoryMovement;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    Page<InventoryMovement> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    Page<InventoryMovement> findByTypeOrderByCreatedAtDesc(InventoryMovement.Type type, Pageable pageable);

    Page<InventoryMovement> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT m FROM InventoryMovement m WHERE m.createdAt BETWEEN :from AND :to ORDER BY m.createdAt DESC")
    Page<InventoryMovement> findByDateRange(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        Pageable pageable);

    @Query("SELECT m FROM InventoryMovement m ORDER BY m.createdAt DESC")
    List<InventoryMovement> findRecent(Pageable pageable);

    @Query("""
        SELECT m FROM InventoryMovement m
        WHERE m.createdAt >= :since
          AND m.type IN ('ENTRY', 'ADJUSTMENT')
        ORDER BY m.createdAt DESC
        """)
    List<InventoryMovement> findRecentEntries(@Param("since") LocalDateTime since);
}