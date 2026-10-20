package com.delosreyes.inventario.modules.auth.entity;

import java.time.LocalDateTime;

import com.delosreyes.inventario.modules.users.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * RefreshToken — Entidad que almacena refresh tokens en base de datos.
 *
 * QUÉ HACE:
 *   Persiste los refresh tokens para poder revocarlos en logout
 *   y emitir nuevos access tokens sin requerir re-login.
 *
 * POR QUÉ EN BD y no solo en cliente:
 *   Si el usuario cierra sesión, el servidor marca revoked=true.
 *   Esto evita que un token robado funcione después del logout.
 *
 * SEGURIDAD:
 *   - Cada token tiene TTL (7 días)
 *   - Al hacer logout se marca revoked=true
 *   - Al renovar access token se valida que no esté revocado ni expirado
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }

    public Long getId() { return id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}