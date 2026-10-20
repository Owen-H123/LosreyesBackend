package com.delosreyes.inventario.modules.audit.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.delosreyes.inventario.modules.audit.entity.AuditLog;

/**
 * AuditLogRepository — Acceso de solo lectura/escritura a registros de auditoría.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByModuleOrderByCreatedAtDesc(String module, Pageable pageable);

    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<AuditLog> findByUserEmailOrderByCreatedAtDesc(String email, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :from AND :to ORDER BY a.createdAt DESC")
    Page<AuditLog> findByDateRange(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.module = :module AND a.action = :action ORDER BY a.createdAt DESC")
    List<AuditLog> findByModuleAndAction(
        @Param("module") String module,
        @Param("action") String action,
        Pageable pageable);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.module = 'AUTH' AND a.action = 'LOGIN' AND a.createdAt >= :since")
    long countLoginsToday(@Param("since") LocalDateTime since);
}