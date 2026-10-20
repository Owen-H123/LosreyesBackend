package com.delosreyes.inventario.modules.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.delosreyes.inventario.modules.sales.entity.Sale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * SaleDtos — DTOs del módulo de ventas.
 *
 * CreateRequest:
 *   Recibe la venta completa con lista de items.
 *   El frontend envía productId + quantity por cada item.
 *   El backend calcula precios, subtotales y totales.
 *
 * UpdateRequest:
 *   Edición LIMITADA de una venta ya registrada. A propósito NO permite
 *   tocar items/cantidades/precios: esos ya afectaron el stock al crear
 *   la venta, y recalcularlos requeriría un flujo de reversión + nuevo
 *   descuento que no está implementado. Solo se pueden editar campos que
 *   no afectan inventario: cliente, método de pago y notas.
 *
 * SaleResponse:
 *   Retorna la venta completa con número de venta generado,
 *   datos del cliente, totales y detalle de items.
 */
public final class SaleDtos {

    private SaleDtos() {}

  
    // REQUEST: Item de venta

    public static class ItemRequest {

        @NotNull(message = "El producto es obligatorio")
        private Long productId;

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        private Integer quantity;

        @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo")
        private BigDecimal discount = BigDecimal.ZERO;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public BigDecimal getDiscount() { return discount; }
        public void setDiscount(BigDecimal discount) { this.discount = discount; }
    }

    // REQUEST: Crear venta
   
    public static class CreateRequest {

        private Long customerId;

        @NotNull(message = "El método de pago es obligatorio")
        private Sale.PaymentMethod paymentMethod = Sale.PaymentMethod.CASH;

        @NotEmpty(message = "La venta debe tener al menos un producto")
        @Valid
        private List<ItemRequest> items;

        @DecimalMin(value = "0.0", message = "El descuento global no puede ser negativo")
        private BigDecimal globalDiscount = BigDecimal.ZERO;

        private String notes;

        public Long getCustomerId() { return customerId; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }
        public Sale.PaymentMethod getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(Sale.PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
        public List<ItemRequest> getItems() { return items; }
        public void setItems(List<ItemRequest> items) { this.items = items; }
        public BigDecimal getGlobalDiscount() { return globalDiscount; }
        public void setGlobalDiscount(BigDecimal globalDiscount) { this.globalDiscount = globalDiscount; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    // REQUEST: Editar venta (campos seguros, sin impacto en stock)

    public static class UpdateRequest {

        private Long customerId;

        private Sale.PaymentMethod paymentMethod;

        private String notes;

        public Long getCustomerId() { return customerId; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }
        public Sale.PaymentMethod getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(Sale.PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    // RESPONSE: Item de venta
    public static class ItemResponse {
        private Long id;
        private Long productId;
        private String productCode;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal discount;
        private BigDecimal subtotal;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductCode() { return productCode; }
        public void setProductCode(String productCode) { this.productCode = productCode; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public BigDecimal getDiscount() { return discount; }
        public void setDiscount(BigDecimal discount) { this.discount = discount; }
        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    }

    // RESPONSE: Venta completa

    public static class SaleResponse {
        private Long id;
        private String saleNumber;
        private Long customerId;
        private String customerName;
        private Long userId;
        private String userName;
        private Sale.Status status;
        private BigDecimal subtotal;
        private BigDecimal discount;
        private BigDecimal tax;
        private BigDecimal total;
        private Sale.PaymentMethod paymentMethod;
        private String notes;
        private List<ItemResponse> items;
        private LocalDateTime saleDate;
        private LocalDateTime createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getSaleNumber() { return saleNumber; }
        public void setSaleNumber(String saleNumber) { this.saleNumber = saleNumber; }
        public Long getCustomerId() { return customerId; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public Sale.Status getStatus() { return status; }
        public void setStatus(Sale.Status status) { this.status = status; }
        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
        public BigDecimal getDiscount() { return discount; }
        public void setDiscount(BigDecimal discount) { this.discount = discount; }
        public BigDecimal getTax() { return tax; }
        public void setTax(BigDecimal tax) { this.tax = tax; }
        public BigDecimal getTotal() { return total; }
        public void setTotal(BigDecimal total) { this.total = total; }
        public Sale.PaymentMethod getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(Sale.PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public List<ItemResponse> getItems() { return items; }
        public void setItems(List<ItemResponse> items) { this.items = items; }
        public LocalDateTime getSaleDate() { return saleDate; }
        public void setSaleDate(LocalDateTime saleDate) { this.saleDate = saleDate; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}