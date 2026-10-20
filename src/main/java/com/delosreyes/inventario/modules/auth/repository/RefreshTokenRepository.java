package com.delosreyes.inventario.modules.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.delosreyes.inventario.modules.auth.entity.RefreshToken;

/**
 * RefreshTokenRepository — Acceso a datos de refresh tokens.
 *
 * QUÉ HACE:
 *   Persiste y consulta los refresh tokens almacenados en BD.
 *   Permite revocar todos los tokens de un usuario (logout total).
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId")
    void revokeAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId AND rt.revoked = true")
    void deleteRevokedByUserId(@Param("userId") Long userId);
}