package com.delosreyes.inventario.modules.roles.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.delosreyes.inventario.modules.roles.entity.Role;

/**
 * RoleRepository — Acceso a datos de roles del sistema.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);
}