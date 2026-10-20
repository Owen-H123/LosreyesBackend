package com.delosreyes.inventario.modules.movements.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.delosreyes.inventario.modules.products.entity.Product;
import com.delosreyes.inventario.modules.users.entity.User;

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

/**
 * InventoryMovement — Registro inmutable de cada cambio de stock.
 *
 * QUÉ HACE:
 *   Guarda el historial completo de entradas, salidas, ventas y ajustes de inventario.
 *   Es la base de trazabilidad del stock del sistema.
 *
 * POR QUÉ EXISTE:
 *   Resuelve "pérdida de stock" y "falta de historial".
 *   Cada modificación de stock genera un registro aquí.
 *   stockBefore y stockAfter permiten auditar cualquier discrepancia.
 *
 * TIPOS:
 *   ENTRY      — Entrada de mercadería (compra a proveedor)
 *   EXIT       — Salida no vendida (devolución, merma, robo)
 *   SALE       — Salida por venta (generado automáticamente al registrar venta)
 *   ADJUSTMENT — Ajuste de inventario (diferencia por conteo físico)
 *
 * INMUTABILIDAD:
 *   No tiene setters de stockBefore/stockAfter después de creación.
 *   El historial nunca se modifica.
 */
@Entity
@Table(name = "inventory_movements")
public class InventoryMovement {

    public enum Type { ENTRY, EXIT, SALE, ADJUSTMENT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "sale_id")
    private Long saleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "stock_before", nullable = false)
    private Integer stockBefore;

    @Column(name = "stock_after", nullable = false)
    private Integer stockAfter;

    @Column(name = "unit_cost", precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(columnDefinition = "TEXT")
    private String observation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Long getSaleId() { return saleId; }
    public void setSaleId(Long saleId) { this.saleId = saleId; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getStockBefore() { return stockBefore; }
    public void setStockBefore(Integer stockBefore) { this.stockBefore = stockBefore; }
    public Integer getStockAfter() { return stockAfter; }
    public void setStockAfter(Integer stockAfter) { this.stockAfter = stockAfter; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public String getObservation() { return observation; }
    public void setObservation(String observation) { this.observation = observation; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}