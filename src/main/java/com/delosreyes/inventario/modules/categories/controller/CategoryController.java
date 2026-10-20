package com.delosreyes.inventario.modules.categories.controller;

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
import org.springframework.web.bind.annotation.RestController;

import com.delosreyes.inventario.modules.categories.dto.CategoryDtos;
import com.delosreyes.inventario.modules.categories.service.CategoryService;
import com.delosreyes.inventario.shared.constants.AppConstants;
import com.delosreyes.inventario.shared.responses.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * CategoryController — Endpoints REST para categorías de productos.
 *
 * GET  /categories        — listar todas (ADMIN + WORKER)
 * GET  /categories/active — solo activas (ADMIN + WORKER)
 * GET  /categories/{id}   — por ID
 * POST /categories        — crear (ADMIN)
 * PUT  /categories/{id}   — actualizar (ADMIN)
 * DELETE /categories/{id} — desactivar (ADMIN)
 */
@RestController
@RequestMapping("/categories")
@Tag(name = "Categorías", description = "Gestión de categorías de productos")
@SecurityRequirement(name = "Bearer Authentication")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "Listar todas las categorías")
    public ResponseEntity<ApiResponse<List<CategoryDtos.Response>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_LIST, categoryService.findAll()));
    }

    @GetMapping("/active")
    @Operation(summary = "Listar categorías activas")
    public ResponseEntity<ApiResponse<List<CategoryDtos.Response>>> findActive() {
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_LIST, categoryService.findActive()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener categoría por ID")
    public ResponseEntity<ApiResponse<CategoryDtos.Response>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_FOUND, categoryService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','PERM_MANAGE_CATEGORIES')")
    @Operation(summary = "Crear nueva categoría")
    public ResponseEntity<ApiResponse<CategoryDtos.Response>> create(
            @Valid @RequestBody CategoryDtos.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(AppConstants.MSG_CREATED, categoryService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','PERM_MANAGE_CATEGORIES')")
    @Operation(summary = "Actualizar categoría")
    public ResponseEntity<ApiResponse<CategoryDtos.Response>> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryDtos.Request request) {
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_UPDATED, categoryService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','PERM_MANAGE_CATEGORIES')")
    @Operation(summary = "Desactivar categoría")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_DELETED));
    }
}