package com.delosreyes.inventario.modules.vehicles.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.delosreyes.inventario.modules.vehicles.dto.VehicleDtos;
import com.delosreyes.inventario.modules.vehicles.service.VehicleService;
import com.delosreyes.inventario.shared.constants.AppConstants;
import com.delosreyes.inventario.shared.responses.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * VehicleController — Endpoints para gestión de vehículos compatibles.
 *
 * GET  /vehicles                 — Todos los vehículos
 * GET  /vehicles/brands          — Marcas disponibles (autocomplete)
 * GET  /vehicles/brands/{brand}/models — Modelos por marca
 * GET  /vehicles/search?q=       — Búsqueda
 * GET  /vehicles/brand/{brand}   — Por marca
 * GET  /vehicles/product/{id}    — Compatibles con producto
 * POST /vehicles                 — Crear (ADMIN)
 * PUT  /vehicles/{id}            — Actualizar (ADMIN)
 * DELETE /vehicles/{id}          — Eliminar (ADMIN)
 */
@RestController
@RequestMapping("/vehicles")
@Tag(name = "Vehículos", description = "Compatibilidad de vehículos con productos")
@SecurityRequirement(name = "Bearer Authentication")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    @Operation(summary = "Listar todos los vehículos")
    public ResponseEntity<ApiResponse<List<VehicleDtos.Response>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_LIST, vehicleService.findAll()));
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar vehículos por marca o modelo (autocomplete)")
    public ResponseEntity<ApiResponse<List<VehicleDtos.Response>>> search(
            @RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_LIST, vehicleService.search(q)));
    }

    @GetMapping("/brands")
    @Operation(summary = "Obtener marcas distintas")
    public ResponseEntity<ApiResponse<List<String>>> findBrands() {
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_LIST, vehicleService.findBrands()));
    }

    @GetMapping("/brands/{brand}/models")
    @Operation(summary = "Obtener modelos por marca")
    public ResponseEntity<ApiResponse<List<String>>> findModels(@PathVariable String brand) {
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_LIST, vehicleService.findModelsByBrand(brand)));
    }

    @GetMapping("/brand/{brand}")
    @Operation(summary = "Vehículos por marca")
    public ResponseEntity<ApiResponse<List<VehicleDtos.Response>>> findByBrand(@PathVariable String brand) {
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_LIST, vehicleService.findByBrand(brand)));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Vehículos compatibles con un producto")
    public ResponseEntity<ApiResponse<List<VehicleDtos.Response>>> findByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_LIST, vehicleService.findByProduct(productId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear vehículo")
    public ResponseEntity<ApiResponse<VehicleDtos.Response>> create(
            @Valid @RequestBody VehicleDtos.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(AppConstants.MSG_CREATED, vehicleService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar vehículo")
    public ResponseEntity<ApiResponse<VehicleDtos.Response>> update(
            @PathVariable Long id,
            @Valid @RequestBody VehicleDtos.Request request) {
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_UPDATED, vehicleService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar vehículo")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        vehicleService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_DELETED));
    }
}