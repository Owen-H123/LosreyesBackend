package com.delosreyes.inventario.modules.audit.controller;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.delosreyes.inventario.modules.audit.dto.AuditDtos;
import com.delosreyes.inventario.modules.audit.service.AuditQueryService;
import com.delosreyes.inventario.shared.responses.ApiResponse;
import com.delosreyes.inventario.shared.responses.PagedResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * AuditController — Consulta del registro de auditoría.
 *
 * ACCESO: Solo ADMIN
 *
 * ENDPOINTS:
 *   GET /audit                    — Todos los logs (paginado)
 *   GET /audit/user/{userId}      — Logs por usuario
 *   GET /audit/module/{module}    — Logs por módulo (AUTH, PRODUCTS, SALES…)
 *   GET /audit/date-range         — Logs por rango de fechas
 */
@RestController
@RequestMapping("/audit")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','PERM_VIEW_AUDIT')")
@Tag(name = "Auditoría", description = "Trazabilidad de acciones del sistema (solo ADMIN)")
@SecurityRequirement(name = "Bearer Authentication")
public class AuditController {

    private final AuditQueryService auditQueryService;

    public AuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping
    @Operation(summary = "Listar todos los registros de auditoría")
    public ResponseEntity<ApiResponse<PagedResponse<AuditDtos.AuditLogResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success("Auditoría obtenida",
            auditQueryService.findAll(page, size)));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Logs de auditoría por usuario")
    public ResponseEntity<ApiResponse<PagedResponse<AuditDtos.AuditLogResponse>>> findByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success("Auditoría del usuario",
            auditQueryService.findByUser(userId, page, size)));
    }

    @GetMapping("/module/{module}")
    @Operation(summary = "Logs por módulo: AUTH, PRODUCTS, SALES, INVENTORY, USERS, CUSTOMERS")
    public ResponseEntity<ApiResponse<PagedResponse<AuditDtos.AuditLogResponse>>> findByModule(
            @PathVariable String module,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success("Auditoría del módulo " + module,
            auditQueryService.findByModule(module, page, size)));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Logs por rango de fechas")
    public ResponseEntity<ApiResponse<PagedResponse<AuditDtos.AuditLogResponse>>> findByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success("Auditoría por fechas",
            auditQueryService.findByDateRange(from, to, page, size)));
    }
}