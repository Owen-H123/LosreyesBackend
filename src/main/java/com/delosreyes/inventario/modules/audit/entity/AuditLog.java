package com.delosreyes.inventario.modules.audit.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * AuditLog — Registro inmutable de todas las acciones realizadas en el sistema.
 *
 * QUÉ HACE:
 *   Guarda un histórico de quién hizo qué y cuándo.
 *   Es el módulo de trazabilidad del sistema.
 *
 * POR QUÉ EXISTE:
 *   Resuelve el problema de "falta de historial" y "trazabilidad de acciones".
 *   Permite auditar login, cambios de productos, ventas, movimientos de stock.
 *   Tabla solo de inserción — nunca se actualiza ni elimina.
 *
 * INMUTABILIDAD:
 *   No tiene @PreUpdate ni setters de campos de negocio.
 *   Una vez creado el registro, no se modifica.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_email", length = 150)
    private String userEmail;

    @Column(nullable = false, length = 50)
    private String module;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "entity_id", length = 50)
    private String entityId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    // Builder pattern para construcción limpia
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final AuditLog log = new AuditLog();

        public Builder userId(Long v)      { log.userId = v; return this; }
        public Builder userEmail(String v) { log.userEmail = v; return this; }
        public Builder module(String v)    { log.module = v; return this; }
        public Builder action(String v)    { log.action = v; return this; }
        public Builder entityId(String v)  { log.entityId = v; return this; }
        public Builder description(String v){ log.description = v; return this; }
        public Builder metadata(String v)  { log.metadata = v; return this; }
        public Builder ipAddress(String v) { log.ipAddress = v; return this; }
        public AuditLog build()            { return log; }
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUserEmail() { return userEmail; }
    public String getModule() { return module; }
    public String getAction() { return action; }
    public String getEntityId() { return entityId; }
    public String getDescription() { return description; }
    public String getMetadata() { return metadata; }
    public String getIpAddress() { return ipAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}