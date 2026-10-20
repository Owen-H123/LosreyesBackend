package com.delosreyes.inventario.modules.products.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.delosreyes.inventario.modules.categories.entity.Category;
import com.delosreyes.inventario.modules.vehicles.entity.Vehicle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Product — Entidad central del ERP. Representa un repuesto o accesorio.
 *
 * QUÉ HACE:
 *   Almacena toda la información de un producto: precios, stock, categoría,
 *   vehículos compatibles, estado y trazabilidad.
 *
 * RELACIONES:
 *   ManyToOne → Category (categoría del producto)
 *   ManyToMany → Vehicle (compatibilidad con vehículos)
 *   OneToMany → InventoryMovement, SaleItem (historial)
 *
 * SOFT DELETE:
 *   El campo 'deleted' permite eliminar lógicamente sin perder historial.
 *   Los queries de listado siempre filtran WHERE deleted = FALSE.
 *
 * STOCK:
 *   El campo stock se actualiza SOLO a través de InventoryMovementService
 *   para garantizar que cada cambio quede registrado en el historial.
 */
@Entity
@Table(name = "products")
public class Product {

    public enum Status { ACTIVE, INACTIVE, DISCONTINUED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "cost_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "sale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal salePrice = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(name = "minimum_stock", nullable = false)
    private Integer minimumStock = 5;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(nullable = false)
    private boolean deleted = false;

    /** Visible en la tienda online (el producto sigue activo en el ERP). */
    @Column(name = "web_published", nullable = false)
    private boolean webPublished = true;

    @Column(name = "web_featured", nullable = false)
    private boolean webFeatured = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "product_vehicles",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "vehicle_id")
    )
    private Set<Vehicle> vehicles = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public boolean isLowStock() {
        return stock <= minimumStock;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getMinimumStock() { return minimumStock; }
    public void setMinimumStock(Integer minimumStock) { this.minimumStock = minimumStock; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public boolean isDeleted() { return deleted; }
    public boolean isWebPublished() { return webPublished; }
    public void setWebPublished(boolean webPublished) { this.webPublished = webPublished; }
    public boolean isWebFeatured() { return webFeatured; }
    public void setWebFeatured(boolean webFeatured) { this.webFeatured = webFeatured; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Set<Vehicle> getVehicles() { return vehicles; }
    public void setVehicles(Set<Vehicle> vehicles) { this.vehicles = vehicles; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}