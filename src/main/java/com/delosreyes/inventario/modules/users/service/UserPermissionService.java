package com.delosreyes.inventario.modules.users.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delosreyes.inventario.modules.audit.service.AuditService;
import com.delosreyes.inventario.modules.users.dto.UserPermissionDtos;
import com.delosreyes.inventario.modules.users.entity.PermissionCode;
import com.delosreyes.inventario.modules.users.entity.User;
import com.delosreyes.inventario.modules.users.entity.UserPermission;
import com.delosreyes.inventario.modules.users.repository.UserPermissionRepository;
import com.delosreyes.inventario.modules.users.repository.UserRepository;
import com.delosreyes.inventario.shared.constants.AppConstants;
import com.delosreyes.inventario.shared.exceptions.BusinessException;
import com.delosreyes.inventario.shared.exceptions.ResourceNotFoundException;

/**
 * UserPermissionService — Otorga/revoca permisos individuales a trabajadores.
 *
 * REGLAS:
 *   - Solo aplica a usuarios con rol WORKER. Un ADMIN ya tiene todo por rol;
 *     no se le puede (ni tiene sentido) tocar permisos individuales.
 *   - MANAGE_USERS no es delegable: no existe en el catálogo PermissionCode.
 *   - update() REEMPLAZA el set completo de permisos otorgados (no hace
 *     merge parcial) — el checkbox del frontend es la fuente de verdad.
 */
@Service
@Transactional
public class UserPermissionService {

    private static final Map<PermissionCode, String> LABELS = Map.of(
        PermissionCode.VIEW_AUDIT,        "Ver auditoría",
        PermissionCode.VIEW_REPORTS,      "Ver reportes",
        PermissionCode.ADJUST_INVENTORY,  "Ajustar inventario",
        PermissionCode.DELETE_PRODUCTS,   "Eliminar productos",
        PermissionCode.CANCEL_SALES,      "Cancelar ventas",
        PermissionCode.MANAGE_CATEGORIES, "Gestionar categorías",
        PermissionCode.EDIT_SALES,        "Editar ventas"
    );

    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final AuditService auditService;

    public UserPermissionService(UserRepository userRepository,
                                  UserPermissionRepository userPermissionRepository,
                                  AuditService auditService) {
        this.userRepository = userRepository;
        this.userPermissionRepository = userPermissionRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<UserPermissionDtos.PermissionItem> getPermissions(Long userId) {
        User user = findUser(userId);

        Set<PermissionCode> granted = userPermissionRepository.findByUserId(userId).stream()
            .map(UserPermission::getPermissionCode)
            .collect(Collectors.toSet());

        boolean isAdmin = user.getRole().getName().equals(AppConstants.ROLE_ADMIN);

        return Arrays.stream(PermissionCode.values())
            .map(code -> new UserPermissionDtos.PermissionItem(
                code, LABELS.get(code), isAdmin || granted.contains(code)))
            .collect(Collectors.toList());
    }

    public List<UserPermissionDtos.PermissionItem> update(
            Long userId, UserPermissionDtos.UpdateRequest request, User currentUser) {

        User user = findUser(userId);

        if (user.getRole().getName().equals(AppConstants.ROLE_ADMIN)) {
            throw new BusinessException("ADMIN_HAS_ALL_PERMISSIONS",
                "Un administrador ya tiene acceso completo; los permisos individuales solo aplican a trabajadores");
        }

        List<PermissionCode> requested = request.getPermissions() != null
            ? request.getPermissions() : List.of();

        userPermissionRepository.deleteAllByUserId(userId);
        userPermissionRepository.flush();

        for (PermissionCode code : requested) {
            userPermissionRepository.save(new UserPermission(user, code, currentUser.getId()));
        }

        auditService.log(AppConstants.MODULE_USERS, "UPDATE_PERMISSIONS",
            currentUser.getId(), currentUser.getEmail(),
            String.valueOf(userId),
            "Permisos actualizados para " + user.getEmail() + ": " + requested);

        return getPermissions(userId);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
    }
}