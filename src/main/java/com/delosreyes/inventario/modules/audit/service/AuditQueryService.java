package com.delosreyes.inventario.modules.audit.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delosreyes.inventario.modules.audit.dto.AuditDtos;
import com.delosreyes.inventario.modules.audit.entity.AuditLog;
import com.delosreyes.inventario.modules.audit.repository.AuditLogRepository;
import com.delosreyes.inventario.shared.constants.AppConstants;
import com.delosreyes.inventario.shared.responses.PagedResponse;

/**
 * AuditQueryService — Consultas de solo lectura sobre el log de auditoría.
 *
 * QUÉ HACE:
 *   Provee métodos de consulta paginada para el AuditController.
 *   Separado de AuditService (que solo escribe) siguiendo CQRS ligero.
 *
 * POR QUÉ SEPARADO DE AuditService:
 *   AuditService solo escribe (y es @Async).
 *   AuditQueryService solo lee (es @Transactional readOnly).
 *   Separación de responsabilidades clara.
 */
@Service
@Transactional(readOnly = true)
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;

    public AuditQueryService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public PagedResponse<AuditDtos.AuditLogResponse> findAll(int page, int size) {
        size = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PagedResponse.from(auditLogRepository.findAll(pageable).map(this::toResponse));
    }

    public PagedResponse<AuditDtos.AuditLogResponse> findByUser(Long userId, int page, int size) {
        size = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);
        return PagedResponse.from(
            auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toResponse));
    }

    public PagedResponse<AuditDtos.AuditLogResponse> findByModule(String module, int page, int size) {
        size = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);
        return PagedResponse.from(
            auditLogRepository.findByModuleOrderByCreatedAtDesc(module.toUpperCase(), pageable)
                .map(this::toResponse));
    }

    public PagedResponse<AuditDtos.AuditLogResponse> findByDateRange(
            LocalDateTime from, LocalDateTime to, int page, int size) {
        size = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);
        return PagedResponse.from(
            auditLogRepository.findByDateRange(from, to, pageable).map(this::toResponse));
    }

    private AuditDtos.AuditLogResponse toResponse(AuditLog log) {
        AuditDtos.AuditLogResponse r = new AuditDtos.AuditLogResponse();
        r.setId(log.getId());
        r.setUserId(log.getUserId());
        r.setUserEmail(log.getUserEmail());
        r.setModule(log.getModule());
        r.setAction(log.getAction());
        r.setEntityId(log.getEntityId());
        r.setDescription(log.getDescription());
        r.setMetadata(log.getMetadata());
        r.setIpAddress(log.getIpAddress());
        r.setCreatedAt(log.getCreatedAt());
        return r;
    }
}