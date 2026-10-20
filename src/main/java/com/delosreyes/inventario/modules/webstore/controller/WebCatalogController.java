package com.delosreyes.inventario.modules.webstore.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.delosreyes.inventario.modules.webstore.dto.WebstoreDtos.CatalogFilter;
import com.delosreyes.inventario.modules.webstore.dto.WebstoreDtos.CatalogItem;
import com.delosreyes.inventario.modules.webstore.dto.WebstoreDtos.CatalogUpdate;
import com.delosreyes.inventario.modules.webstore.service.WebCatalogService;
import com.delosreyes.inventario.shared.responses.ApiResponse;
import com.delosreyes.inventario.shared.responses.PagedResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Catálogo web desde el ERP (requiere login: ADMIN o WORKER). */
@RestController
@RequestMapping("/web-catalog")
@Tag(name = "Catálogo web", description = "Publicar, ocultar y destacar productos en la tienda online")
public class WebCatalogController {

    private final WebCatalogService catalog;

    public WebCatalogController(WebCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    @Operation(summary = "Productos activos y su estado en la tienda online")
    public ResponseEntity<ApiResponse<PagedResponse<CatalogItem>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean published,
            @RequestParam(defaultValue = "false") boolean featured,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Catálogo web",
            catalog.list(new CatalogFilter(search, published, featured), page, size)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Publicar, ocultar o destacar un producto en la tienda online")
    public ResponseEntity<ApiResponse<CatalogItem>> update(@PathVariable Long id, @RequestBody CatalogUpdate req) {
        return ResponseEntity.ok(ApiResponse.success("Producto actualizado", catalog.update(id, req)));
    }
}
