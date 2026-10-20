package com.delosreyes.inventario.modules.webstore.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.delosreyes.inventario.modules.webstore.dto.WebstoreDtos.SettingsRequest;
import com.delosreyes.inventario.modules.webstore.dto.WebstoreDtos.SettingsResponse;
import com.delosreyes.inventario.modules.webstore.service.StoreSettingsService;
import com.delosreyes.inventario.shared.responses.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Ajustes de la tienda online.
 *   GET /public/store-settings → la web los lee sin login
 *   GET /store-settings        → el equipo (ADMIN o WORKER)
 *   PUT /store-settings        → solo ADMIN (ver SecurityConfig)
 */
@RestController
@Tag(name = "Ajustes de tienda", description = "WhatsApp, medios de pago, Yape y datos de entrega")
public class StoreSettingsController {

    private final StoreSettingsService settings;

    public StoreSettingsController(StoreSettingsService settings) {
        this.settings = settings;
    }

    @GetMapping("/public/store-settings")
    @Operation(summary = "Ajustes públicos de la tienda")
    public ResponseEntity<ApiResponse<SettingsResponse>> publicSettings() {
        return ResponseEntity.ok(ApiResponse.success("Ajustes", settings.get()));
    }

    @GetMapping("/store-settings")
    @Operation(summary = "Ajustes de la tienda")
    public ResponseEntity<ApiResponse<SettingsResponse>> get() {
        return ResponseEntity.ok(ApiResponse.success("Ajustes", settings.get()));
    }

    @PutMapping("/store-settings")
    @Operation(summary = "Guardar los ajustes de la tienda (solo ADMIN)")
    public ResponseEntity<ApiResponse<SettingsResponse>> save(@Valid @RequestBody SettingsRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Ajustes guardados", settings.save(req)));
    }
}
