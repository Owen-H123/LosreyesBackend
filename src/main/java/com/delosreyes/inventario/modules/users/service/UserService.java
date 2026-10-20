package com.delosreyes.inventario.modules.users.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delosreyes.inventario.modules.audit.service.AuditService;
import com.delosreyes.inventario.modules.roles.entity.Role;
import com.delosreyes.inventario.modules.roles.repository.RoleRepository;
import com.delosreyes.inventario.modules.users.dto.UserDtos;
import com.delosreyes.inventario.modules.users.entity.User;
import com.delosreyes.inventario.modules.users.mapper.UserMapper;
import com.delosreyes.inventario.modules.users.repository.UserRepository;
import com.delosreyes.inventario.shared.constants.AppConstants;
import com.delosreyes.inventario.shared.exceptions.BusinessException;
import com.delosreyes.inventario.shared.exceptions.DuplicateResourceException;
import com.delosreyes.inventario.shared.exceptions.ResourceNotFoundException;
import com.delosreyes.inventario.shared.responses.PagedResponse;

/**
 * UserService — Lógica de negocio para gestión de usuarios del sistema.
 *
 * QUÉ HACE:
 *   CRUD completo de usuarios: crear trabajadores, editar, activar/desactivar.
 *   Solo el ADMIN puede ejecutar estas operaciones (validado en SecurityConfig y @PreAuthorize).
 *
 * REGLAS DE NEGOCIO:
 *   - No se puede eliminar el único administrador activo del sistema.
 *   - No se puede duplicar email de usuario.
 *   - La contraseña siempre se encripta con BCrypt antes de persistir.
 *   - Al desactivar un usuario, sus refresh tokens se revocan (no puede re-autenticarse).
 *
 * RELACIONES:
 *   UserController → UserService → UserRepository + RoleRepository
 *   UserService → AuditService (trazabilidad)
 *   UserService → UserMapper (conversión entity ↔ DTO)
 */
@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }


    // CREAR USUARIO

    public UserDtos.UserResponse create(UserDtos.CreateRequest request, User currentUser) {
        validateEmailUnique(request.getEmail(), null);

        Role role = findRole(request.getRoleId());

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);

        User saved = userRepository.save(user);

        auditService.log(AppConstants.MODULE_USERS, AppConstants.ACTION_CREATE,
            currentUser.getId(), currentUser.getEmail(),
            String.valueOf(saved.getId()),
            "Usuario creado: " + saved.getEmail() + " [" + role.getName() + "]");

        log.info("Usuario creado: {} [{}] por {}", saved.getEmail(), role.getName(), currentUser.getEmail());
        return userMapper.toResponse(saved);
    }


    // ACTUALIZAR USUARIO

    public UserDtos.UserResponse update(Long id, UserDtos.UpdateRequest request, User currentUser) {
        User user = findById(id);
        validateEmailUnique(request.getEmail(), id);

        // No permitir desactivar al último admin
        if (request.getStatus() != User.Status.ACTIVE
                && user.getRole().getName().equals(AppConstants.ROLE_ADMIN)) {
            long activeAdmins = userRepository.countActiveByRole(AppConstants.ROLE_ADMIN);
            if (activeAdmins <= 1) {
                throw new BusinessException("LAST_ADMIN",
                    "No se puede desactivar al único administrador activo del sistema");
            }
        }

        Role role = findRole(request.getRoleId());
        userMapper.updateEntity(request, user);
        user.setRole(role);

        User saved = userRepository.save(user);

        auditService.log(AppConstants.MODULE_USERS, AppConstants.ACTION_UPDATE,
            currentUser.getId(), currentUser.getEmail(),
            String.valueOf(saved.getId()),
            "Usuario actualizado: " + saved.getEmail() + " estado=" + saved.getStatus());

        return userMapper.toResponse(saved);
    }


    // ELIMINAR USUARIO (soft: desactivar)

    public void delete(Long id, User currentUser) {
        User user = findById(id);

        if (user.getId().equals(currentUser.getId())) {
            throw new BusinessException("SELF_DELETE", "No puede eliminar su propia cuenta");
        }

        if (user.getRole().getName().equals(AppConstants.ROLE_ADMIN)) {
            long activeAdmins = userRepository.countActiveByRole(AppConstants.ROLE_ADMIN);
            if (activeAdmins <= 1) {
                throw new BusinessException("LAST_ADMIN",
                    "No se puede eliminar al único administrador del sistema");
            }
        }

        user.setStatus(User.Status.INACTIVE);
        userRepository.save(user);

        auditService.log(AppConstants.MODULE_USERS, AppConstants.ACTION_DELETE,
            currentUser.getId(), currentUser.getEmail(),
            String.valueOf(id), "Usuario desactivado: " + user.getEmail());
    }

    // ACTIVAR / SUSPENDER

    public UserDtos.UserResponse changeStatus(Long id, User.Status newStatus, User currentUser) {
        User user = findById(id);

        if (newStatus == User.Status.INACTIVE || newStatus == User.Status.SUSPENDED) {
            if (user.getRole().getName().equals(AppConstants.ROLE_ADMIN)) {
                long activeAdmins = userRepository.countActiveByRole(AppConstants.ROLE_ADMIN);
                if (activeAdmins <= 1) {
                    throw new BusinessException("LAST_ADMIN",
                        "No se puede suspender al único administrador activo");
                }
            }
        }

        user.setStatus(newStatus);
        User saved = userRepository.save(user);

        auditService.log(AppConstants.MODULE_USERS, AppConstants.ACTION_UPDATE,
            currentUser.getId(), currentUser.getEmail(),
            String.valueOf(id), "Estado cambiado a: " + newStatus);

        return userMapper.toResponse(saved);
    }

    // CONSULTAS (solo lectura)

    @Transactional(readOnly = true)
    public UserDtos.UserResponse findByIdDto(Long id) {
        return userMapper.toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserDtos.UserResponse> findAll(String search, int page, int size) {
        size = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<UserDtos.UserResponse> result;
        if (search != null && !search.isBlank()) {
            result = userRepository
                .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search, pageable)
                .map(userMapper::toResponse);
        } else {
            result = userRepository.findAll(pageable).map(userMapper::toResponse);
        }

        return PagedResponse.from(result);
    }

    // Helpers privados

    private User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
    }

    private Role findRole(Long roleId) {
        return roleRepository.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Rol", "id", roleId));
    }

    private void validateEmailUnique(String email, Long excludeId) {
        userRepository.findByEmail(email).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new DuplicateResourceException("Usuario", "email", email);
            }
        });
    }
}