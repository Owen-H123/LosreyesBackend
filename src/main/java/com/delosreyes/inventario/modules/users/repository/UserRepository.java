package com.delosreyes.inventario.modules.users.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.delosreyes.inventario.modules.users.entity.User;

/**
 * UserRepository — Acceso a datos de usuarios.
 *
 * QUÉ HACE:
 *   Provee operaciones CRUD y consultas específicas para usuarios.
 *   Spring Data JPA genera la implementación automáticamente.
 *
 * RELACIONES:
 *   Usado por UserService y UserDetailsServiceImpl para cargar usuarios.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String name, String email, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.status = :status ORDER BY u.createdAt DESC")
    Page<User> findByStatus(@Param("status") User.Status status, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = :roleName AND u.status = 'ACTIVE'")
    long countActiveByRole(@Param("roleName") String roleName);
}