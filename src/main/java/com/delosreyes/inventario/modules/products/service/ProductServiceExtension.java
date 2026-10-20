package com.delosreyes.inventario.modules.products.service;

/**
 * ProductServiceExtension — Métodos adicionales del ProductService usados por DashboardService.
 *
 * NOTA: Estos métodos se agregan directamente en ProductService.
 * Este archivo documenta los métodos que deben añadirse al final de ProductService.java:
 *
 *   public long countLowStock() {
 *       return productRepository.countLowStock();
 *   }
 *
 *   public long countActive() {
 *       return productRepository.countActive();
 *   }
 *
 * Ya están declarados en ProductRepository como:
 *   @Query("SELECT COUNT(p) FROM Product p WHERE p.deleted = FALSE AND p.stock <= p.minimumStock AND p.status = 'ACTIVE'")
 *   long countLowStock();
 *
 *   @Query("SELECT COUNT(p) FROM Product p WHERE p.deleted = FALSE AND p.status = 'ACTIVE'")
 *   long countActive();
 */
public class ProductServiceExtension {
    // Ver ProductService.java para implementación completa.
    // Los métodos countLowStock() y countActive() están integrados directamente allí.
}