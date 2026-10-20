package com.delosreyes.inventario.modules.sales.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.delosreyes.inventario.modules.sales.dto.SaleDtos;
import com.delosreyes.inventario.modules.sales.entity.Sale;
import com.delosreyes.inventario.modules.sales.service.SaleService;
import com.delosreyes.inventario.modules.users.entity.User;
import com.delosreyes.inventario.shared.constants.AppConstants;
import com.delosreyes.inventario.shared.responses.ApiResponse;
import com.delosreyes.inventario.shared.responses.PagedResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * SaleController — Endpoints REST del módulo de ventas.
 *
 * ACCESO:
 *   POST /sales                — ADMIN + WORKER (registrar venta)
 *   GET  /sales                — ADMIN + WORKER (historial)
 *   GET  /sales/{id}           — ADMIN + WORKER
 *   GET  /sales/number/{num}   — ADMIN + WORKER
 *   GET  /sales/recent         — ADMIN + WORKER
 *   POST /sales/{id}/cancel    — Solo ADMIN
 */
@RestController
@RequestMapping("/sales")
@Tag(name = "Ventas", description = "Registro y consulta de ventas")
@SecurityRequirement(name = "Bearer Authentication")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    @Operation(summary = "Registrar nueva venta con descuento de stock automático")
    public ResponseEntity<ApiResponse<SaleDtos.SaleResponse>> create(
            @Valid @RequestBody SaleDtos.CreateRequest request,
            @AuthenticationPrincipal User currentUser) {

        SaleDtos.SaleResponse sale = saleService.create(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Venta registrada exitosamente", sale));
    }

    @GetMapping
    @Operation(summary = "Listar ventas con filtros opcionales")
    public ResponseEntity<ApiResponse<PagedResponse<SaleDtos.SaleResponse>>> findAll(
            @RequestParam(required = false) Sale.Status status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_LIST,
            saleService.findAll(status, customerId, userId, from, to, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener venta por ID")
    public ResponseEntity<ApiResponse<SaleDtos.SaleResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_FOUND, saleService.findById(id)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','PERM_EDIT_SALES')")
    @Operation(summary = "Editar cliente, método de pago o notas de una venta (solo ADMIN, no toca items/stock)")
    public ResponseEntity<ApiResponse<SaleDtos.SaleResponse>> update(
            @PathVariable Long id,
            @RequestBody SaleDtos.UpdateRequest request,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(ApiResponse.success("Venta actualizada exitosamente",
            saleService.update(id, request, currentUser)));
    }

    @GetMapping("/number/{saleNumber}")
    @Operation(summary = "Obtener venta por número (ej: VTA-20240115-00001)")
    public ResponseEntity<ApiResponse<SaleDtos.SaleResponse>> findBySaleNumber(
            @PathVariable String saleNumber) {
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_FOUND,
            saleService.findBySaleNumber(saleNumber)));
    }

    @GetMapping("/recent")
    @Operation(summary = "Últimas ventas (para dashboard)")
    public ResponseEntity<ApiResponse<List<SaleDtos.SaleResponse>>> findRecent(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_LIST,
            saleService.findRecent(limit)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','PERM_CANCEL_SALES')")
    @Operation(summary = "Cancelar venta y revertir stock (solo ADMIN)")
    public ResponseEntity<ApiResponse<SaleDtos.SaleResponse>> cancel(
            @PathVariable Long id,
            @RequestParam String reason,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(ApiResponse.success("Venta cancelada exitosamente",
            saleService.cancel(id, reason, currentUser)));
    }
}