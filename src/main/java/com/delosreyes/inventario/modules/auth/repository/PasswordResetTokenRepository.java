package com.delosreyes.inventario.modules.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.delosreyes.inventario.modules.auth.entity.PasswordResetToken;

/**
 * PasswordResetTokenRepository — Acceso a las solicitudes de recuperación de contraseña.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    // Al pedir un enlace nuevo, los anteriores sin usar dejan de servir
    @Modifying
    @Query("""
        UPDATE PasswordResetToken t SET t.usedAt = :now
        WHERE t.user.id = :userId AND t.usedAt IS NULL
        """)
    void invalidateAllByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
