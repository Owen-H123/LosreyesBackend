package com.delosreyes.inventario.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Auth DTOs — Objetos de transferencia para autenticación.
 *
 * QUÉ HACEN:
 *   Definen el contrato de entrada/salida para los endpoints de auth.
 *   Separan el modelo de BD (User entity) del modelo de la API.
 *
 * POR QUÉ CLASES ESTÁTICAS ANIDADAS:
 *   Agrupa todos los DTOs de auth en un solo archivo para mejor organización.
 *   Evita proliferación de archivos pequeños relacionados.
 */
public final class AuthDtos {

    private AuthDtos() {}

    // REQUEST: Login

    public static class LoginRequest {

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Formato de email inválido")
        private String email;

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    // RESPONSE: Login exitoso

    public static class LoginResponse {

        private Long userId;
        private String name;
        private String email;
        private String role;
        private String avatarUrl;
        private java.util.List<String> permissions;
        private String accessToken;
        private String refreshToken;
        private long expiresIn;

        public LoginResponse() {}

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private final LoginResponse r = new LoginResponse();

            public Builder userId(Long v) { r.userId = v; return this; }
            public Builder name(String v) { r.name = v; return this; }
            public Builder email(String v) { r.email = v; return this; }
            public Builder role(String v) { r.role = v; return this; }
            public Builder avatarUrl(String v) { r.avatarUrl = v; return this; }
            public Builder permissions(java.util.List<String> v) { r.permissions = v; return this; }
            public Builder accessToken(String v) { r.accessToken = v; return this; }
            public Builder refreshToken(String v) { r.refreshToken = v; return this; }
            public Builder expiresIn(long v) { r.expiresIn = v; return this; }
            public LoginResponse build() { return r; }
        }

        public Long getUserId() { return userId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getAvatarUrl() { return avatarUrl; }
        public java.util.List<String> getPermissions() { return permissions; }
        public String getAccessToken() { return accessToken; }
        // No se envía en el JSON: viaja en una cookie HttpOnly (RefreshTokenCookie)
        @JsonIgnore
        public String getRefreshToken() { return refreshToken; }
        public long getExpiresIn() { return expiresIn; }
    }

    // REQUEST: Forgot password

    public static class ForgotPasswordRequest {

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Formato de email inválido")
        private String email;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    // REQUEST: Restablecer contraseña con el token del correo

    public static class ResetPasswordRequest {

        @NotBlank(message = "El enlace de recuperación no es válido")
        private String token;

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres")
        private String newPassword;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    // REQUEST: Change password

    public static class ChangePasswordRequest {

        @NotBlank(message = "La contraseña actual es obligatoria")
        private String currentPassword;

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres")
        private String newPassword;

        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    // REQUEST: Actualizar perfil propio (nombre / email)

    public static class UpdateProfileRequest {

        @NotBlank(message = "El nombre es obligatorio")
        private String name;

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Formato de email inválido")
        private String email;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}