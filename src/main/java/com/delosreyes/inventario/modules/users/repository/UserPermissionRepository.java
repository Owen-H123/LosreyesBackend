package com.delosreyes.inventario.modules.users.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.delosreyes.inventario.modules.users.entity.UserPermission;

public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {

    List<UserPermission> findByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM UserPermission up WHERE up.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}