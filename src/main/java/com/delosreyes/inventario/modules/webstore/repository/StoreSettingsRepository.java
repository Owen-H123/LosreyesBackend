package com.delosreyes.inventario.modules.webstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.delosreyes.inventario.modules.webstore.entity.StoreSettings;

public interface StoreSettingsRepository extends JpaRepository<StoreSettings, Short> {
}
