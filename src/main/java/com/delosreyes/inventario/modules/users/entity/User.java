package com.delosreyes.inventario.modules.users.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.delosreyes.inventario.modules.roles.entity.Role;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * User — Entidad central del sistema de autenticación y autorización.
 *
 * QUÉ HACE:
 *   Representa a un usuario del sistema (administrador o trabajador).
 *   Implementa UserDetails para integrarse directamente con Spring Security.
 *
 * POR QUÉ IMPLEMENTA UserDetails:
 *   Spring Security necesita esta interfaz para cargar el usuario en el SecurityContext.
 *   Permite que JwtAuthenticationFilter autentique con los datos reales de la BD.
 *
 * ESTADOS:
 *   ACTIVE     — puede iniciar sesión y operar
 *   SUSPENDED  — bloqueado temporalmente (cuenta bloqueada = isAccountNonLocked=false)
 *   INACTIVE   — desactivado permanentemente (cuenta deshabilitada = isEnabled=false)
 *
 * RELACIONES:
 *   ManyToOne con Role (un usuario tiene un rol)
 *   OneToMany con Sales, Movements, AuditLogs (trazabilidad)
 */
@Entity
@Table(name = "users")
public class User implements UserDetails {

    public enum Status { ACTIVE, SUSPENDED, INACTIVE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserPermission> permissions = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // UserDetails — Spring Security Integration

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));

        // Permisos individuales otorgados por un ADMIN (solo aplica a WORKER;
        // ADMIN ya tiene acceso total por su rol, no necesita filas aquí).
        // Se re-cargan desde BD en cada request (JwtAuthenticationFilter llama
        // loadUserByUsername en cada llamada), así que un cambio de permisos
        // aplica de inmediato, sin que el trabajador tenga que re-loguearse.
        for (UserPermission up : permissions) {
            authorities.add(new SimpleGrantedAuthority("PERM_" + up.getPermissionCode().name()));
        }

        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != Status.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == Status.ACTIVE;
    }

    // Getters & Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public List<String> getPermissionCodes() {
        return permissions.stream()
            .map(p -> p.getPermissionCode().name())
            .collect(java.util.stream.Collectors.toList());
    }

    public List<UserPermission> getPermissions() {
        return permissions;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}