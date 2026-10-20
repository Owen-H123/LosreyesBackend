package com.delosreyes.inventario.modules.receipts.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ReceiptDtos — Contenedor de DTOs del módulo de boletas.
 *
 * QUÉ HACE:
 *   Agrupa las clases de transporte de datos necesarias para construir
 *   una boleta (PDF y, a futuro, correo) a partir de una venta existente.
 *   No reemplaza a SaleDtos: es una vista de presentación orientada a impresión.
 */
public class ReceiptDtos {

    private ReceiptDtos() {}

    // ============================================================
    // Datos completos de la boleta
    // ============================================================
    public static class ReceiptData {

        private Long saleId;
        private String saleNumber;
        private LocalDateTime saleDate;
        private String status;
        private String paymentMethod;

        // Vendedor
        private String sellerName;
        private String sellerEmail;

        // Cliente
        private String customerName;
        private String customerDocumentType;
        private String customerDocumentNumber;
        private String customerEmail;
        private String customerAddress;

        // Totales
        private BigDecimal subtotal;
        private BigDecimal discount;
        private BigDecimal tax;
        private BigDecimal total;
        private String notes;

        // Items
        private List<ReceiptItem> items;

        public Long getSaleId() { return saleId; }
        public void setSaleId(Long saleId) { this.saleId = saleId; }

        public String getSaleNumber() { return saleNumber; }
        public void setSaleNumber(String saleNumber) { this.saleNumber = saleNumber; }

        public LocalDateTime getSaleDate() { return saleDate; }
        public void setSaleDate(LocalDateTime saleDate) { this.saleDate = saleDate; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

        public String getSellerName() { return sellerName; }
        public void setSellerName(String sellerName) { this.sellerName = sellerName; }

        public String getSellerEmail() { return sellerEmail; }
        public void setSellerEmail(String sellerEmail) { this.sellerEmail = sellerEmail; }

        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }

        public String getCustomerDocumentType() { return customerDocumentType; }
        public void setCustomerDocumentType(String customerDocumentType) { this.customerDocumentType = customerDocumentType; }

        public String getCustomerDocumentNumber() { return customerDocumentNumber; }
        public void setCustomerDocumentNumber(String customerDocumentNumber) { this.customerDocumentNumber = customerDocumentNumber; }

        public String getCustomerEmail() { return customerEmail; }
        public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

        public String getCustomerAddress() { return customerAddress; }
        public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }

        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

        public BigDecimal getDiscount() { return discount; }
        public void setDiscount(BigDecimal discount) { this.discount = discount; }

        public BigDecimal getTax() { return tax; }
        public void setTax(BigDecimal tax) { this.tax = tax; }

        public BigDecimal getTotal() { return total; }
        public void setTotal(BigDecimal total) { this.total = total; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public List<ReceiptItem> getItems() { return items; }
        public void setItems(List<ReceiptItem> items) { this.items = items; }
    }

    // ============================================================
    // Ítem de la boleta (producto vendido)
    // ============================================================
    public static class ReceiptItem {

        private String productCode;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal discount;
        private BigDecimal subtotal;

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
}