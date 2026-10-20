package com.delosreyes.inventario.modules.products.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.delosreyes.inventario.modules.products.dto.ProductDtos;
import com.delosreyes.inventario.modules.products.entity.Product;
import com.delosreyes.inventario.modules.products.service.ProductImageService;
import com.delosreyes.inventario.modules.products.service.ProductService;
import com.delosreyes.inventario.modules.users.entity.User;
import com.delosreyes.inventario.shared.constants.AppConstants;
import com.delosreyes.inventario.shared.responses.ApiResponse;
import com.delosreyes.inventario.shared.responses.PagedResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * ProductController — Endpoints REST del módulo de productos.
 *
 * ACCESO:
 *   GET (todos)            — ADMIN + WORKER
 *   POST, PUT, PATCH       — ADMIN + WORKER (crear y editar)
 *   DELETE                 — Solo ADMIN (configurado en SecurityConfig)
 *
 * ENDPOINTS:
 *   GET    /products                    — Listar (paginado, filtros, búsqueda)
 *   GET    /products/{id}               — Por ID
 *   GET    /products/code/{code}        — Por código
 *   GET    /products/low-stock          — Stock bajo
 *   GET    /products/vehicle/{id}       — Por vehículo compatible
 *   POST   /products                    — Crear
 *   PUT    /products/{id}               — Actualizar
 *   PATCH  /products/{id}/status        — Cambiar estado
 *   DELETE /products/{id}               — Eliminar (soft, solo ADMIN)
 */
@RestController
@RequestMapping("/products")
@Tag(name = "Productos", description = "Gestión de repuestos y accesorios automotrices")
@SecurityRequirement(name = "Bearer Authentication")
public class ProductController {

    private final ProductService productService;
    private final ProductImageService productImageService;

    public ProductController(ProductService productService, ProductImageService productImageService) {
        this.productService = productService;
        this.productImageService = productImageService;
    }

    @GetMapping
    @Operation(summary = "Listar productos con filtros y paginación")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDtos.ProductResponse>>> findAll(
            @Parameter(description = "Búsqueda por código, nombre o marca")
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Product.Status status,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponse<ProductDtos.ProductResponse> result =
            productService.findAll(search, categoryId, status, sortBy, sortDir, page, size);
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_LIST, result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID")
    public ResponseEntity<ApiResponse<ProductDtos.ProductResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_FOUND, productService.findById(id)));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Obtener producto por código (búsqueda rápida en caja)")
    public ResponseEntity<ApiResponse<ProductDtos.ProductResponse>> findByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_FOUND, productService.findByCode(code)));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Productos con stock bajo o agotado")
    public ResponseEntity<ApiResponse<List<ProductDtos.ProductResponse>>> findLowStock() {
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_LIST, productService.findLowStock()));
    }

    @GetMapping("/vehicle/{vehicleId}")
    @Operation(summary = "Productos compatibles con un vehículo específico")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDtos.ProductResponse>>> findByVehicle(
            @PathVariable Long vehicleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_LIST,
            productService.findByVehicle(vehicleId, page, size)));
    }

    @PostMapping
    @Operation(summary = "Crear nuevo producto")
    public ResponseEntity<ApiResponse<ProductDtos.ProductResponse>> create(
            @Valid @RequestBody ProductDtos.CreateRequest request,
            @AuthenticationPrincipal User currentUser) {

        ProductDtos.ProductResponse created = productService.create(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(AppConstants.MSG_CREATED, created));
    }

    @PostMapping("/upload-image")
    @Operation(summary = "Subir imagen de producto (jpg/png/webp, máx 5MB) y obtener su URL")
    public ResponseEntity<ApiResponse<UploadImageResponse>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        String imageUrl = productImageService.store(file);
        return ResponseEntity.ok(
                ApiResponse.success("Imagen subida exitosamente", new UploadImageResponse(imageUrl)));
    }

    public record UploadImageResponse(String imageUrl) {}

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto existente")
    public ResponseEntity<ApiResponse<ProductDtos.ProductResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductDtos.UpdateRequest request,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_UPDATED,
            productService.update(id, request, currentUser)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','PERM_DELETE_PRODUCTS')")
    @Operation(summary = "Eliminar producto (soft delete, solo ADMIN)")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        productService.delete(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_DELETED));
    }
}