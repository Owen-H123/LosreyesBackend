package com.delosreyes.inventario.modules.roles.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.delosreyes.inventario.modules.roles.entity.Role;
import com.delosreyes.inventario.modules.roles.repository.RoleRepository;
import com.delosreyes.inventario.shared.responses.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * RoleController — Consulta de roles disponibles del sistema.
 *
 * ACCESO: Solo ADMIN
 *
 * ENDPOINTS:
 *   GET /roles — Listar todos los roles (para selector al crear usuarios)
 */
@RestController
@RequestMapping("/roles")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Roles", description = "Gestión de roles del sistema")
@SecurityRequirement(name = "Bearer Authentication")
public class RoleController {

    private final RoleRepository roleRepository;

    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GetMapping
    @Operation(summary = "Listar todos los roles disponibles")
    public ResponseEntity<ApiResponse<List<Role>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success("Roles obtenidos", roleRepository.findAll()));
    }
}