package com.delosreyes.inventario.modules.sales.entity;

import com.delosreyes.inventario.modules.customers.entity.Customer;
import com.delosreyes.inventario.modules.users.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Sale — Entidad de venta completa con sus items.
 *
 * QUÉ HACE:
 *   Representa una transacción de venta con todos sus detalles.
 *   Mantiene subtotal, descuento, IGV y total calculados.
 *
 * RELACIONES:
 *   ManyToOne → Customer (puede ser null para venta sin cliente)
 *   ManyToOne → User (quién realizó la venta)
 *   OneToMany → SaleItem (detalle de productos vendidos)
 *
 * FLUJO DE CREACIÓN:
 *   1. SaleService valida productos y stock
 *   2. Calcula totales
 *   3. Persiste Sale + SaleItems en una sola transacción
 *   4. Llama a InventoryMovementService por cada item
 *   5. Registra auditoría
 */
@Entity
@Table(name = "sales")
public class Sale {

    public enum Status { PENDING, COMPLETED, CANCELLED, REFUNDED }
    public enum PaymentMethod { CASH, CARD, TRANSFER, CREDIT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_number", nullable = false, unique = true, length = 20)
    private String saleNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.COMPLETED;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "sale_date", nullable = false)
    private LocalDateTime saleDate;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (saleDate == null) saleDate = LocalDateTime.now();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public void addItem(SaleItem item) {
        item.setSale(this);
        items.add(item);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSaleNumber() { return saleNumber; }
    public void setSaleNumber(String saleNumber) { this.saleNumber = saleNumber; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }
    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDateTime saleDate) { this.saleDate = saleDate; }
    public List<SaleItem> getItems() { return items; }
    public void setItems(List<SaleItem> items) { this.items = items; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}