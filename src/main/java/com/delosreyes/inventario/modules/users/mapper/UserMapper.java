package com.delosreyes.inventario.modules.users.mapper;

import com.delosreyes.inventario.modules.users.dto.UserDtos;
import com.delosreyes.inventario.modules.users.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * UserMapper — Conversión entre User entity y DTOs usando MapStruct.
 *
 * QUÉ HACE:
 *   Genera automáticamente código de mapeo entre entidades y DTOs.
 *   Evita código repetitivo de asignación campo por campo.
 *
 * POR QUÉ MAPSTRUCT:
 *   Genera código en tiempo de compilación (no reflection = más rápido que ModelMapper).
 *   Errores de mapeo se detectan en compilación, no en runtime.
 *
 * RELACIONES:
 *   UserService lo usa para convertir resultados antes de retornarlos al Controller.
 *
 * SEGURIDAD:
 *   El mapeo a UserResponse NUNCA incluye el campo password.
 *   MapStruct solo mapea campos declarados en el DTO destino.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", source = "role.name")
    @Mapping(target = "roleId", source = "role.id")
    UserDtos.UserResponse toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "lastLogin", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "password", ignore = true)
    User toEntity(UserDtos.CreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UserDtos.UpdateRequest request, @MappingTarget User user);
}