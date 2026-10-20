package com.delosreyes.inventario.modules.users.dto;

import java.time.LocalDateTime;

import com.delosreyes.inventario.modules.users.entity.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * UserDtos — DTOs para el módulo de usuarios.
 *
 * QUÉ HACEN:
 *   Separan el modelo de BD del contrato de la API.
 *   CreateRequest y UpdateRequest validan entradas.
 *   UserResponse expone solo campos necesarios (nunca la contraseña).
 *
 * SEGURIDAD:
 *   UserResponse NO incluye el campo password bajo ninguna circunstancia.
 */
public final class UserDtos {

    private UserDtos() {}


    // REQUEST: Crear usuario

    public static class CreateRequest {

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        private String name;

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Formato de email inválido")
        @Size(max = 150, message = "El email no debe superar 150 caracteres")
        private String email;

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "La contraseña debe tener al menos una mayúscula, una minúscula y un número"
        )
        private String password;

        @NotNull(message = "El rol es obligatorio")
        private Long roleId;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public Long getRoleId() { return roleId; }
        public void setRoleId(Long roleId) { this.roleId = roleId; }
    }

    // REQUEST: Actualizar usuario

    public static class UpdateRequest {

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        private String name;

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Formato de email inválido")
        private String email;

        @NotNull(message = "El rol es obligatorio")
        private Long roleId;

        @NotNull(message = "El estado es obligatorio")
        private User.Status status;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Long getRoleId() { return roleId; }
        public void setRoleId(Long roleId) { this.roleId = roleId; }
        public User.Status getStatus() { return status; }
        public void setStatus(User.Status status) { this.status = status; }
    }

    // RESPONSE: Datos del usuario (nunca expone password)
    public static class UserResponse {

        private Long id;
        private String name;
        private String email;
        private String role;
        private Long roleId;
        private String avatarUrl;
        private User.Status status;
        private LocalDateTime lastLogin;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public Long getRoleId() { return roleId; }
        public void setRoleId(Long roleId) { this.roleId = roleId; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        public User.Status getStatus() { return status; }
        public void setStatus(User.Status status) { this.status = status; }
        public LocalDateTime getLastLogin() { return lastLogin; }
        public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
}