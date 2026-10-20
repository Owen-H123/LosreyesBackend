package com.delosreyes.inventario.modules.products.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.delosreyes.inventario.modules.products.entity.Product;
import com.delosreyes.inventario.modules.vehicles.dto.VehicleDtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * ProductDtos — DTOs completos del módulo de productos.
 *
 * Incluye:
 *   CreateRequest   — para POST /products
 *   UpdateRequest   — para PUT /products/{id}
 *   ProductResponse — respuesta con datos completos
 *   ProductFilter   — parámetros de búsqueda/filtrado
 */
public final class ProductDtos {

    private ProductDtos() {}

    // ============================================================
    // REQUEST: Crear producto
    // ============================================================
    public static class CreateRequest {

        @NotBlank(message = "El código del producto es obligatorio")
        @Size(min = 2, max = 50, message = "El código debe tener entre 2 y 50 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9\\-_]+$", message = "El código solo puede contener letras, números, guiones y guiones bajos")
        private String code;

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 200, message = "El nombre debe tener entre 2 y 200 caracteres")
        private String name;

        @Size(max = 2000, message = "La descripción no debe superar 2000 caracteres")
        private String description;

        @Size(max = 100, message = "La marca no debe superar 100 caracteres")
        private String brand;

        @NotNull(message = "La categoría es obligatoria")
        private Long categoryId;

        @NotNull(message = "El precio de costo es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio de costo debe ser mayor a 0")
        @Digits(integer = 10, fraction = 2, message = "Formato de precio inválido")
        private BigDecimal costPrice;

        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio de venta debe ser mayor a 0")
        @Digits(integer = 10, fraction = 2, message = "Formato de precio inválido")
        private BigDecimal salePrice;

        @NotNull(message = "El stock inicial es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        private Integer stock;

        @Min(value = 0, message = "El stock mínimo no puede ser negativo")
        private Integer minimumStock = 5;

        private String imageUrl;

        private Set<Long> vehicleIds;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
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
        public Set<Long> getVehicleIds() { return vehicleIds; }
        public void setVehicleIds(Set<Long> vehicleIds) { this.vehicleIds = vehicleIds; }
    }

    // ============================================================
    // REQUEST: Actualizar producto
    // ============================================================
    public static class UpdateRequest {

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 200)
        private String name;

        @Size(max = 2000)
        private String description;

        @Size(max = 100)
        private String brand;

        @NotNull(message = "La categoría es obligatoria")
        private Long categoryId;

        @NotNull(message = "El precio de costo es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal costPrice;

        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal salePrice;

        @Min(value = 0)
        private Integer minimumStock;

        private String imageUrl;

        @NotNull(message = "El estado es obligatorio")
        private Product.Status status;

        private Set<Long> vehicleIds;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public BigDecimal getCostPrice() { return costPrice; }
        public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
        public BigDecimal getSalePrice() { return salePrice; }
        public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
        public Integer getMinimumStock() { return minimumStock; }
        public void setMinimumStock(Integer minimumStock) { this.minimumStock = minimumStock; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public Product.Status getStatus() { return status; }
        public void setStatus(Product.Status status) { this.status = status; }
        public Set<Long> getVehicleIds() { return vehicleIds; }
        public void setVehicleIds(Set<Long> vehicleIds) { this.vehicleIds = vehicleIds; }
    }

    // ============================================================
    // RESPONSE: Datos completos del producto
    // ============================================================
    public static class ProductResponse {

        private Long id;
        private String code;
        private String name;
        private String description;
        private String brand;
        private Long categoryId;
        private String categoryName;
        private BigDecimal costPrice;
        private BigDecimal salePrice;
        private BigDecimal profitMargin;
        private Integer stock;
        private Integer minimumStock;
        private boolean lowStock;
        private String imageUrl;
        private Product.Status status;
        private List<VehicleDtos.Response> vehicles;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

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
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public BigDecimal getCostPrice() { return costPrice; }
        public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
        public BigDecimal getSalePrice() { return salePrice; }
        public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
        public BigDecimal getProfitMargin() { return profitMargin; }
        public void setProfitMargin(BigDecimal profitMargin) { this.profitMargin = profitMargin; }
        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }
        public Integer getMinimumStock() { return minimumStock; }
        public void setMinimumStock(Integer minimumStock) { this.minimumStock = minimumStock; }
        public boolean isLowStock() { return lowStock; }
        public void setLowStock(boolean lowStock) { this.lowStock = lowStock; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public Product.Status getStatus() { return status; }
        public void setStatus(Product.Status status) { this.status = status; }
        public List<VehicleDtos.Response> getVehicles() { return vehicles; }
        public void setVehicles(List<VehicleDtos.Response> vehicles) { this.vehicles = vehicles; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
}