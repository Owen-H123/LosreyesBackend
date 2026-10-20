package com.delosreyes.inventario.modules.audit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.delosreyes.inventario.modules.audit.entity.AuditLog;
import com.delosreyes.inventario.modules.audit.repository.AuditLogRepository;

/**
 * AuditService — Servicio de registro de auditoría asíncrono.
 *
 * QUÉ HACE:
 *   Guarda registros de auditoría sin bloquear el flujo principal de negocio.
 *   Cada acción importante del sistema llama a este servicio.
 *
 * POR QUÉ @ASYNC:
 *   La auditoría no debe ralentizar las operaciones principales.
 *   Si falla el registro de auditoría, la operación principal no debe fallar.
 *
 * POR QUÉ PROPAGATION.REQUIRES_NEW:
 *   Usa su propia transacción. Si la transacción principal hace rollback,
 *   el log de auditoría se guarda de todas formas (para rastrear el intento fallido).
 *
 * USOS:
 *   authService.login → auditService.log(AUTH, LOGIN, userId, "Login exitoso")
 *   productService.create → auditService.log(PRODUCTS, CREATE, productId, ...)
 *   saleService.create → auditService.log(SALES, CREATE, saleId, ...)
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String module, String action, Long userId, String userEmail,
                    String entityId, String description) {
        log(module, action, userId, userEmail, entityId, description, null, null);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String module, String action, Long userId, String userEmail,
                    String entityId, String description, String metadata, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                .module(module)
                .action(action)
                .userId(userId)
                .userEmail(userEmail)
                .entityId(entityId)
                .description(description)
                .metadata(metadata)
                .ipAddress(ipAddress)
                .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Error guardando log de auditoría [{}/{}]: {}", module, action, e.getMessage());
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLogin(Long userId, String email, String ipAddress) {
        log("AUTH", "LOGIN", userId, email, String.valueOf(userId),
            "Inicio de sesión exitoso", null, ipAddress);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLogout(Long userId, String email) {
        log("AUTH", "LOGOUT", userId, email, String.valueOf(userId),
            "Cierre de sesión", null, null);
    }
}