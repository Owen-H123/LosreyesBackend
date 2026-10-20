package com.delosreyes.inventario.modules.movements.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.delosreyes.inventario.modules.movements.entity.InventoryMovement;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class MovementDtos {

    private MovementDtos() {}

    // ============================================================
    // REQUEST: Registrar movimiento manual (ENTRY, EXIT, ADJUSTMENT)
    // ============================================================
    public static class CreateRequest {

        @NotNull(message = "El producto es obligatorio")
        private Long productId;

        // Sin @NotNull: el tipo no viene del cliente, lo asigna InventoryController
        // según la URL (/entry → ENTRY, /exit → EXIT). @Valid se ejecuta antes que
        // el controller, así que con @NotNull toda petición del frontend fallaba.
        private InventoryMovement.Type type;

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor a 0")
        private Integer quantity;

        @DecimalMin(value = "0.0", message = "El costo unitario no puede ser negativo")
        private BigDecimal unitCost;

        @Size(max = 500, message = "La observación no debe superar 500 caracteres")
        private String observation;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public InventoryMovement.Type getType() { return type; }
        public void setType(InventoryMovement.Type type) { this.type = type; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public BigDecimal getUnitCost() { return unitCost; }
        public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
        public String getObservation() { return observation; }
        public void setObservation(String observation) { this.observation = observation; }
    }

    // ============================================================
    // RESPONSE
    // ============================================================
    public static class MovementResponse {

        private Long id;
        private Long productId;
        private String productCode;
        private String productName;
        private Long userId;
        private String userName;
        private Long saleId;
        private InventoryMovement.Type type;
        private Integer quantity;
        private Integer stockBefore;
        private Integer stockAfter;
        private BigDecimal unitCost;
        private String observation;
        private LocalDateTime createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductCode() { return productCode; }
        public void setProductCode(String productCode) { this.productCode = productCode; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public Long getSaleId() { return saleId; }
        public void setSaleId(Long saleId) { this.saleId = saleId; }
        public InventoryMovement.Type getType() { return type; }
        public void setType(InventoryMovement.Type type) { this.type = type; }
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
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}