package com.delosreyes.inventario.modules.inventory.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.delosreyes.inventario.modules.movements.dto.MovementDtos;
import com.delosreyes.inventario.modules.movements.entity.InventoryMovement;
import com.delosreyes.inventario.modules.movements.service.InventoryMovementService;
import com.delosreyes.inventario.modules.users.entity.User;
import com.delosreyes.inventario.shared.constants.AppConstants;
import com.delosreyes.inventario.shared.responses.ApiResponse;
import com.delosreyes.inventario.shared.responses.PagedResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * InventoryController — Endpoints de gestión de inventario y movimientos.
 *
 * ENDPOINTS:
 *   POST   /inventory/entry           — Entrada de mercadería (ADMIN + WORKER)
 *   POST   /inventory/exit            — Salida manual (ADMIN + WORKER)
 *   POST   /inventory/adjustment      — Ajuste de stock (solo ADMIN)
 *   GET    /inventory/movements       — Listar movimientos (paginado)
 *   GET    /inventory/movements/product/{id} — Por producto
 *   GET    /inventory/movements/recent — Últimos movimientos (dashboard)
 *   GET    /inventory/movements/date-range  — Por rango de fechas
 */
@RestController
@RequestMapping("/inventory")
@Tag(name = "Inventario", description = "Entradas, salidas y ajustes de stock")
@SecurityRequirement(name = "Bearer Authentication")
public class InventoryController {

    private final InventoryMovementService movementService;

    public InventoryController(InventoryMovementService movementService) {
        this.movementService = movementService;
    }

    @PostMapping("/entry")
    @Operation(summary = "Registrar entrada de mercadería (compra a proveedor)")
    public ResponseEntity<ApiResponse<MovementDtos.MovementResponse>> registerEntry(
            @Valid @RequestBody MovementDtos.CreateRequest request,
            @AuthenticationPrincipal User currentUser) {

        request.setType(InventoryMovement.Type.ENTRY);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Entrada registrada exitosamente",
                    movementService.register(request, currentUser)));
    }

    @PostMapping("/exit")
    @Operation(summary = "Registrar salida manual (devolución, merma)")
    public ResponseEntity<ApiResponse<MovementDtos.MovementResponse>> registerExit(
            @Valid @RequestBody MovementDtos.CreateRequest request,
            @AuthenticationPrincipal User currentUser) {

        request.setType(InventoryMovement.Type.EXIT);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Salida registrada exitosamente",
                    movementService.register(request, currentUser)));
    }

    @PostMapping("/adjustment")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','PERM_ADJUST_INVENTORY')")
    @Operation(summary = "Ajustar stock por conteo físico (solo ADMIN)")
    public ResponseEntity<ApiResponse<MovementDtos.MovementResponse>> adjustment(
            @RequestParam @NotNull Long productId,
            @RequestParam @NotNull @Min(0) Integer newStock,
            @RequestParam(required = false) String observation,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ajuste registrado exitosamente",
                    movementService.registerAdjustment(productId, newStock, observation, currentUser)));
    }

    @GetMapping("/movements")
    @Operation(summary = "Listar movimientos de inventario")
    public ResponseEntity<ApiResponse<PagedResponse<MovementDtos.MovementResponse>>> findAll(
            @RequestParam(required = false) InventoryMovement.Type type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_LIST,
            movementService.findAll(type, page, size)));
    }

    @GetMapping("/movements/product/{productId}")
    @Operation(summary = "Historial de movimientos por producto")
    public ResponseEntity<ApiResponse<PagedResponse<MovementDtos.MovementResponse>>> findByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_LIST,
            movementService.findByProduct(productId, page, size)));
    }

    @GetMapping("/movements/recent")
    @Operation(summary = "Últimos movimientos (para dashboard)")
    public ResponseEntity<ApiResponse<List<MovementDtos.MovementResponse>>> findRecent(
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_LIST,
            movementService.findRecent(limit)));
    }

    @GetMapping("/movements/date-range")
    @Operation(summary = "Movimientos por rango de fechas")
    public ResponseEntity<ApiResponse<PagedResponse<MovementDtos.MovementResponse>>> findByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_LIST,
            movementService.findByDateRange(from, to, page, size)));
    }
}