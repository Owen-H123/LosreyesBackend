package com.delosreyes.inventario.modules.customers.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.delosreyes.inventario.modules.customers.dto.CustomerDtos;
import com.delosreyes.inventario.modules.customers.service.CustomerService;
import com.delosreyes.inventario.shared.constants.AppConstants;
import com.delosreyes.inventario.shared.responses.ApiResponse;
import com.delosreyes.inventario.shared.responses.PagedResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * CustomerController — Endpoints para gestión de clientes.
 *
 * GET    /customers                  — Listar / buscar
 * GET    /customers/{id}             — Por ID
 * GET    /customers/document/{doc}   — Por número de documento
 * POST   /customers                  — Crear
 * PUT    /customers/{id}             — Actualizar
 * DELETE /customers/{id}             — Desactivar
 */
@RestController
@RequestMapping("/customers")
@Tag(name = "Clientes", description = "Gestión de clientes")
@SecurityRequirement(name = "Bearer Authentication")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @Operation(summary = "Listar clientes con búsqueda opcional")
    public ResponseEntity<ApiResponse<PagedResponse<CustomerDtos.Response>>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_LIST,
            customerService.findAll(search, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener cliente por ID")
    public ResponseEntity<ApiResponse<CustomerDtos.Response>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_FOUND, customerService.findById(id)));
    }

    @GetMapping("/document/{documentNumber}")
    @Operation(summary = "Buscar cliente por número de documento (rápido en punto de venta)")
    public ResponseEntity<ApiResponse<CustomerDtos.Response>> findByDocument(
            @PathVariable String documentNumber) {
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_FOUND,
            customerService.findByDocument(documentNumber)));
    }

    @PostMapping
    @Operation(summary = "Crear nuevo cliente")
    public ResponseEntity<ApiResponse<CustomerDtos.Response>> create(
            @Valid @RequestBody CustomerDtos.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(AppConstants.MSG_CREATED, customerService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar cliente")
    public ResponseEntity<ApiResponse<CustomerDtos.Response>> update(
            @PathVariable Long id,
            @Valid @RequestBody CustomerDtos.Request request) {
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_UPDATED,
            customerService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar cliente")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        customerService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_DELETED));
    }
}