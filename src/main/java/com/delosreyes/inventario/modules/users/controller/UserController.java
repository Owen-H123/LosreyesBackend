package com.delosreyes.inventario.modules.users.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.delosreyes.inventario.modules.users.dto.UserDtos;
import com.delosreyes.inventario.modules.users.dto.UserPermissionDtos;
import com.delosreyes.inventario.modules.users.entity.User;
import com.delosreyes.inventario.modules.users.service.UserPermissionService;
import com.delosreyes.inventario.modules.users.service.UserService;
import com.delosreyes.inventario.shared.constants.AppConstants;
import com.delosreyes.inventario.shared.responses.ApiResponse;
import com.delosreyes.inventario.shared.responses.PagedResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * UserController — Endpoints REST para gestión de usuarios.
 *
 * ACCESO: Solo ADMIN (configurado en SecurityConfig + @PreAuthorize).
 *
 * RESPONSABILIDAD:
 *   - Recibir y validar HTTP requests
 *   - Inyectar el usuario autenticado (@AuthenticationPrincipal)
 *   - Delegar al UserService
 *   - Retornar ApiResponse estandarizado
 *
 * ENDPOINTS:
 *   GET    /users              — Listar usuarios (paginado + búsqueda)
 *   GET    /users/{id}         — Obtener usuario por ID
 *   POST   /users              — Crear usuario
 *   PUT    /users/{id}         — Actualizar usuario
 *   DELETE /users/{id}         — Desactivar usuario
 *   PATCH  /users/{id}/status  — Cambiar estado del usuario
 */
@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Usuarios", description = "Gestión de usuarios y trabajadores (solo ADMIN)")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;
    private final UserPermissionService userPermissionService;

    public UserController(UserService userService, UserPermissionService userPermissionService) {
        this.userService = userService;
        this.userPermissionService = userPermissionService;
    }

    @GetMapping
    @Operation(summary = "Listar usuarios con paginación y búsqueda opcional")
    public ResponseEntity<ApiResponse<PagedResponse<UserDtos.UserResponse>>> findAll(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponse<UserDtos.UserResponse> result = userService.findAll(search, page, size);
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_LIST, result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID")
    public ResponseEntity<ApiResponse<UserDtos.UserResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_FOUND, userService.findByIdDto(id)));
    }

    @PostMapping
    @Operation(summary = "Crear nuevo usuario o trabajador")
    public ResponseEntity<ApiResponse<UserDtos.UserResponse>> create(
            @Valid @RequestBody UserDtos.CreateRequest request,
            @AuthenticationPrincipal User currentUser) {

        UserDtos.UserResponse created = userService.create(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(AppConstants.MSG_CREATED, created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar datos de usuario")
    public ResponseEntity<ApiResponse<UserDtos.UserResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UserDtos.UpdateRequest request,
            @AuthenticationPrincipal User currentUser) {

        UserDtos.UserResponse updated = userService.update(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_UPDATED, updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar usuario (soft delete)")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        userService.delete(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(AppConstants.MSG_DELETED));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cambiar estado del usuario: ACTIVE, SUSPENDED, INACTIVE")
    public ResponseEntity<ApiResponse<UserDtos.UserResponse>> changeStatus(
            @PathVariable Long id,
            @RequestParam User.Status status,
            @AuthenticationPrincipal User currentUser) {

        UserDtos.UserResponse updated = userService.changeStatus(id, status, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Estado actualizado exitosamente", updated));
    }

    @GetMapping("/{id}/permissions")
    @Operation(summary = "Ver permisos individuales de un trabajador")
    public ResponseEntity<ApiResponse<List<UserPermissionDtos.PermissionItem>>> getPermissions(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                AppConstants.MSG_FOUND,
                userPermissionService.getPermissions(id)));
    }

    @PutMapping("/{id}/permissions")
    @Operation(summary = "Otorgar/revocar permisos individuales a un trabajador")
    public ResponseEntity<ApiResponse<List<UserPermissionDtos.PermissionItem>>> updatePermissions(
            @PathVariable Long id,
            @RequestBody UserPermissionDtos.UpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                "Permisos actualizados exitosamente",
                userPermissionService.update(id, request, currentUser)));
    }
}