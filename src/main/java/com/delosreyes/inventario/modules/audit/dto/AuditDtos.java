package com.delosreyes.inventario.modules.audit.dto;

import java.time.LocalDateTime;

/**
 * AuditDtos — DTOs de respuesta del módulo de auditoría.
 */
public final class AuditDtos {

    private AuditDtos() {}

    public static class AuditLogResponse {
        private Long id;
        private Long userId;
        private String userEmail;
        private String module;
        private String action;
        private String entityId;
        private String description;
        private String metadata;
        private String ipAddress;
        private LocalDateTime createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUserEmail() { return userEmail; }
        public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
        public String getModule() { return module; }
        public void setModule(String module) { this.module = module; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getEntityId() { return entityId; }
        public void setEntityId(String entityId) { this.entityId = entityId; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getMetadata() { return metadata; }
        public void setMetadata(String metadata) { this.metadata = metadata; }
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}