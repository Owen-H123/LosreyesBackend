package com.delosreyes.inventario.modules.users.entity;

import java.time.LocalDateTime;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * UserPermission — Un permiso individual otorgado a un usuario WORKER.
 *
 * La presencia de una fila = el usuario tiene ese permiso extra.
 * Se borra la fila para revocarlo (no hay un flag "granted=false").
 */
@Entity
@Table(name = "user_permissions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "permission_code"}))
public class UserPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_code", nullable = false, length = 50)
    private PermissionCode permissionCode;

    @Column(name = "granted_by")
    private Long grantedBy;

    @Column(name = "granted_at", nullable = false, updatable = false)
    private LocalDateTime grantedAt;

    @PrePersist
    protected void onCreate() { grantedAt = LocalDateTime.now(); }

    public UserPermission() {}

    public UserPermission(User user, PermissionCode permissionCode, Long grantedBy) {
        this.user = user;
        this.permissionCode = permissionCode;
        this.grantedBy = grantedBy;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public PermissionCode getPermissionCode() { return permissionCode; }
    public void setPermissionCode(PermissionCode permissionCode) { this.permissionCode = permissionCode; }
    public Long getGrantedBy() { return grantedBy; }
    public void setGrantedBy(Long grantedBy) { this.grantedBy = grantedBy; }
    public LocalDateTime getGrantedAt() { return grantedAt; }
}